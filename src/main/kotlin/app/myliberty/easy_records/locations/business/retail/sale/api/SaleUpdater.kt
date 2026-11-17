package app.myliberty.easy_records.locations.business.retail.sale.api

import app.myliberty.easy_records.util.exceptions.RtsGenericException
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.util.enums.PaymentStatus
import app.myliberty.easy_records.locations.business.retail.sale.SaleAssembler
import app.myliberty.easy_records.locations.business.retail.sale.SaleEntity
import app.myliberty.easy_records.locations.business.retail.sale.SaleLocker
import app.myliberty.easy_records.locations.business.retail.sale.SaleRepository
import app.myliberty.easy_records.locations.business.retail.sale.SaleTaxFinalizer
import app.myliberty.easy_records.locations.business.retail.sale.SaleValidator
import app.myliberty.easy_records.locations.business.retail.sale.SaleVoidEntity
import app.myliberty.easy_records.locations.business.retail.sale.VoidedSale
import app.myliberty.easy_records.locations.business.retail.sale.SaleVoidHandlerForKafka
import app.myliberty.easy_records.locations.business.retail.sale.SaleVoidRepository
import app.myliberty.easy_records.locations.business.retail.stock.api.updater.SaleStockUpdater
import app.myliberty.easy_records.locations.business.retail.stock_reservation.api.StockReserver
import app.myliberty.easy_records.organizations.business.fiscal_period.api.FiscalPeriodService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class SaleUpdater(
    private val saleLocker: SaleLocker,
    private val saleRepository: SaleRepository,
    private val stockReserver: StockReserver,
    private val saleAssembler: SaleAssembler,
    private val saleStockUpdater: SaleStockUpdater,
    private val saleVoidHandlerForKafka: SaleVoidHandlerForKafka,
    private val saleValidator: SaleValidator,
    private val saleVoidRepository: SaleVoidRepository,
    private val fiscalPeriodService: FiscalPeriodService,
    private val saleTaxFinalizer: SaleTaxFinalizer,
) {

    fun updatePaymentStatus(id: UUID, status: PaymentStatus): Long =
        updatePaymentStatuses(mapOf(id to status)).getValue(id)

    fun updatePaymentStatuses(paymentStatusBySaleId: Map<UUID, PaymentStatus>): Map<UUID, Long> {
        val sales = saleLocker.lockAndGetSales(paymentStatusBySaleId.keys.toList())
        sales.forEach { it.paymentStatus = paymentStatusBySaleId.getValue(it.id!!) }
        return saleRepository.saveAllAndFlush(sales).associate { it.id!! to it.version }
    }

    fun updateNotes(id: UUID, notes: String?): Long {
        val sale = saleLocker.lockAndGetSale(id)
        sale.notes = notes
        return saleRepository.saveAndFlush(sale).version
    }

    fun updateNotesAtVersion(id: UUID, notes: String?, expectedVersion: Long?): Long {
        val sale = saleLocker.lockAndGetSale(id)
        if (expectedVersion == null || sale.version != expectedVersion) {
            throw RtsGenericException(
                "Sale ${sale.requiredReference()} has been modified since you opened it — please reload and try again"
            )
        }
        sale.notes = notes
        return saleRepository.saveAndFlush(sale).version
    }

    fun voidSourcesOfConsolidation(sourceSaleIds: List<UUID>, consolidatedSaleReferenceNumber: String) {
        val sourceSales = saleLocker.lockAndGetSales(sourceSaleIds)
        voidLockedSales(sourceSales, consolidationVoidReason(consolidatedSaleReferenceNumber))
    }

    fun voidSale(saleVoidCreateDto: SaleVoidCreateDto): SaleSummary {
        val sale = saleLocker.lockAndGetSale(saleVoidCreateDto.saleId)
        voidLockedSales(listOf(sale), saleVoidCreateDto.reason)
        return saleAssembler.buildSummary(sale)
    }

    private fun voidLockedSales(sales: List<SaleEntity>, reason: String) {
        if (sales.isEmpty()) return
        saleValidator.guardCanVoid(sales)
        val (draftSales, confirmedSales) = sales.partition { it.status == SaleStatus.DRAFT }
        stockReserver.clearBySales(draftSales.map { it.id!! })
        draftSales.forEach { draftSale ->
            draftSale.status = SaleStatus.DISCARDED
            draftSale.notes = notesWithDiscardReason(draftSale.notes, reason)
        }
        if (confirmedSales.isNotEmpty()) voidConfirmedSales(confirmedSales, reason)
        saleRepository.saveAll(sales)
    }

    // A discarded draft has no void record, so the reason is kept where the sale's other notes are.
    private fun notesWithDiscardReason(existingNotes: String?, reason: String): String? {
        val discardNote = StringUtils.getValueOrNull(reason)?.let { "Discarded: $it" } ?: return existingNotes
        return listOfNotNull(StringUtils.getValueOrNull(existingNotes), discardNote).joinToString("\n")
    }

    private fun voidConfirmedSales(confirmedSales: List<SaleEntity>, reason: String) {
        val saleVoidEntityBySaleId = saleVoidRepository
            .saveAll(confirmedSales.map { SaleVoidEntity(it.id!!, reason) })
            .associateBy { it.saleId }
        val voidedSales = confirmedSales.map { VoidedSale(it, saleVoidEntityBySaleId.getValue(it.id!!)) }
        voidedSales.map { it.voidDate }.distinct().forEach { fiscalPeriodService.requireOpenForDate(it) }
        confirmedSales.forEach { sale ->
            saleStockUpdater.restoreStock(sale.requiredReference())
            sale.status = SaleStatus.VOIDED
        }
        saleTaxFinalizer.finalizeTaxForVoids(voidedSales)
        saleVoidHandlerForKafka.publishVoids(voidedSales)
    }

    companion object {
        fun consolidationVoidReason(consolidatedSaleReferenceNumber: String): String =
            "Consolidated into $consolidatedSaleReferenceNumber"
    }
}
