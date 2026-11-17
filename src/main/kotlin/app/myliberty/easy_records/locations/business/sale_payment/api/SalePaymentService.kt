package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleUpdater
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentMapper
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentRepository
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentVoidEntity
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentVoidHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentVoidRepository
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentWriter
import me.ezra_home.retail_software_solution.locations.business.sale_payment.VoidedSalePayment
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodDataFetcher
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.PaymentStatusResolver
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnLocationSchema
class SalePaymentService(
    private val salePaymentRepository: SalePaymentRepository,
    private val salePaymentVoidRepository: SalePaymentVoidRepository,
    private val saleDataFetcher: SaleDataFetcher,
    private val saleUpdater: SaleUpdater,
    private val salePaymentFetcher: SalePaymentFetcher,
    private val salePaymentWriter: SalePaymentWriter,
    private val salePaymentVoidHandlerForKafka: SalePaymentVoidHandlerForKafka,
    private val paymentMethodDataFetcher: PaymentMethodDataFetcher,
    private val fiscalPeriodService: FiscalPeriodService,
) {

    fun recordPayment(salePaymentCreateDto: SalePaymentCreateDto): SalePaymentResponseDto {
        val saleId = salePaymentCreateDto.saleId ?: throw RtsGenericException("saleId is required")
        val effectivePaymentDate = salePaymentCreateDto.paymentDate ?: DateTimes.Offset.Now.organization()
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.atOrganizationZone(effectivePaymentDate))
        val (contactId, receivableTotal, saleStatus) = saleDataFetcher.lockAndGetSaleContext(saleId)
        SalePaymentValidator.guardOpenForPayment(saleStatus)
        val alreadyPaid = salePaymentFetcher.calculatePaidAmount(saleId)
        SalePaymentValidator.guardNotExceedingBalance(salePaymentCreateDto.amount.toBigDecimal(), receivableTotal.subtract(alreadyPaid))
        val writeResult = salePaymentWriter.write(
            saleId = saleId,
            contactId = contactId,
            receivableTotal = receivableTotal,
            newSalePayments = listOf(
                SalePaymentWriter.NewSalePayment(
                    paymentMethodId = salePaymentCreateDto.paymentMethodId,
                    amount = salePaymentCreateDto.amount.toBigDecimal(),
                    reference = salePaymentCreateDto.reference,
                    paymentDate = effectivePaymentDate,
                )
            ),
        )
        val savedSalePayment = writeResult.savedSalePayments.single()
        val updatedSaleVersion = saleUpdater.updatePaymentStatus(saleId, writeResult.newPaymentStatus)
        return SalePaymentMapper.toResponseDto(
            savedSalePayment,
            null,
            paymentMethodDataFetcher.getNamesById(),
            writeResult.newPaymentStatus,
            updatedSaleVersion,
        )
    }

    fun voidPayment(salePaymentVoidCreateDto: SalePaymentVoidCreateDto): SalePaymentResponseDto =
        voidPayments(listOf(salePaymentVoidCreateDto)).single()

    fun voidPayments(salePaymentVoidCreateDtos: List<SalePaymentVoidCreateDto>): List<SalePaymentResponseDto> {
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.Now.organization())
        val reasonBySalePaymentId = salePaymentVoidCreateDtos.associate { it.salePaymentId to it.reason }
        if (reasonBySalePaymentId.size != salePaymentVoidCreateDtos.size) {
            throw RtsGenericException("The same payment was selected to void more than once")
        }
        val payments = salePaymentRepository.findAllById(reasonBySalePaymentId.keys)
        if (payments.size != reasonBySalePaymentId.size) {
            throw RtsGenericException("Some payments to void no longer exist")
        }
        val paymentsBySaleId = payments.groupBy { it.saleId }
        val saleContextBySaleId = saleDataFetcher.lockAndGetSaleContexts(paymentsBySaleId.keys.toList())
        val alreadyVoidedPaymentIds = salePaymentVoidRepository
            .findBySalePaymentIdIn(payments.map { it.id!! })
            .map { it.salePaymentId }
            .toSet()
        payments.forEach {
            SalePaymentValidator.guardNotAlreadyVoided(it.id!! in alreadyVoidedPaymentIds, it.requiredReference())
        }
        saleContextBySaleId.values.forEach { SalePaymentValidator.guardSaleNotVoided(it.status) }

        val paidBySaleId = salePaymentFetcher.calculatePaidAmounts(saleContextBySaleId.keys.toList())
        val voidEntityByPaymentId = salePaymentVoidRepository
            .saveAll(payments.map { SalePaymentVoidEntity(it.id!!, reasonBySalePaymentId.getValue(it.id!!)) })
            .associateBy { it.salePaymentId }
        val paymentMethodNamesById = paymentMethodDataFetcher.getNamesById()
        val paymentStatusBySaleId = saleContextBySaleId.mapValues { (saleId, saleContext) ->
            val totalPaidAfterVoid = PaymentStatusResolver.paidTotalAfterVoid(
                paidBySaleId[saleId] ?: BigDecimal.ZERO, paymentsBySaleId.getValue(saleId).sumOf { it.amount }
            )
            PaymentStatusResolver.resolve(totalPaidAfterVoid, saleContext.receivableTotal)
        }
        val updatedSaleVersionBySaleId = saleUpdater.updatePaymentStatuses(paymentStatusBySaleId)
        return saleContextBySaleId.flatMap { (saleId, saleContext) ->
            val salePayments = paymentsBySaleId.getValue(saleId)
            salePaymentVoidHandlerForKafka.publish(
                saleContext.contactId,
                salePayments.map { VoidedSalePayment(it, voidEntityByPaymentId.getValue(it.id!!)) },
            )
            salePayments.map {
                SalePaymentMapper.toResponseDto(
                    it,
                    reasonBySalePaymentId.getValue(it.id!!),
                    paymentMethodNamesById,
                    paymentStatusBySaleId.getValue(saleId),
                    updatedSaleVersionBySaleId.getValue(saleId),
                )
            }
        }
    }
}
