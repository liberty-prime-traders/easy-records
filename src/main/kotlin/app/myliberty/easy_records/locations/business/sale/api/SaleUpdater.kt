package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale.SaleAssembler
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLocker
import me.ezra_home.retail_software_solution.locations.business.sale.SaleRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleTaxFinalizer
import me.ezra_home.retail_software_solution.locations.business.sale.SaleValidator
import me.ezra_home.retail_software_solution.locations.business.sale.SaleVoidEntity
import me.ezra_home.retail_software_solution.locations.business.sale.VoidedSale
import me.ezra_home.retail_software_solution.locations.business.sale.SaleVoidHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale.SaleVoidRepository
import me.ezra_home.retail_software_solution.locations.business.stock.api.updater.SaleStockUpdater
import me.ezra_home.retail_software_solution.locations.business.stock_reservation.api.StockReserver
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
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
