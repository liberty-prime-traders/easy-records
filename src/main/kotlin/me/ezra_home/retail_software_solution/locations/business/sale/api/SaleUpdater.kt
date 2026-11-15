package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale.SaleAssembler
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleTaxFinalizer
import me.ezra_home.retail_software_solution.locations.business.sale.SaleValidator
import me.ezra_home.retail_software_solution.locations.business.sale.SaleVoidEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleVoidHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale.SaleVoidRepository
import me.ezra_home.retail_software_solution.locations.business.stock.api.updater.SaleStockUpdater
import me.ezra_home.retail_software_solution.locations.business.stock_reservation.api.StockReserver
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class SaleUpdater(
    private val saleDataFetcher: SaleDataFetcher,
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

    fun updatePaymentStatus(id: UUID, status: PaymentStatus): Long {
        val sale = saleDataFetcher.lockAndGetSale(id)
        sale.paymentStatus = status
        return saleRepository.saveAndFlush(sale).version
    }

    fun updateNotes(id: UUID, notes: String?) {
        val sale = saleDataFetcher.lockAndGetSale(id)
        sale.notes = notes
        saleRepository.save(sale)
    }

    fun voidSourceOfConsolidation(sourceSaleId: UUID, consolidatedSaleReferenceNumber: String): SaleSummary {
        val sourceSale = saleDataFetcher.lockAndGetSale(sourceSaleId)
        if (sourceSale.status == SaleStatus.DISCARDED) return saleAssembler.buildSummary(sourceSale)
        return voidLockedSale(sourceSale, consolidationVoidReason(consolidatedSaleReferenceNumber))
    }

    fun voidSale(saleVoidCreateDto: SaleVoidCreateDto): SaleSummary =
        voidLockedSale(saleDataFetcher.lockAndGetSale(saleVoidCreateDto.saleId), saleVoidCreateDto.reason)

    private fun voidLockedSale(sale: SaleEntity, reason: String): SaleSummary {
        saleValidator.guardCanVoid(sale)
        val saleId = sale.id!!
        if (sale.status == SaleStatus.DRAFT) {
            stockReserver.clearBySale(saleId)
            sale.status = SaleStatus.DISCARDED
        } else {
            val saleVoidEntity = saleVoidRepository.save(SaleVoidEntity(saleId = saleId, reason = reason))
            val voidDate = DateTimes.Local.atOrganizationZone(saleVoidEntity.requiredCreatedOn())
            fiscalPeriodService.requireOpenForDate(voidDate)
            saleStockUpdater.restoreStock(sale.requiredReference())
            sale.status = SaleStatus.VOIDED
            saleTaxFinalizer.finalizeTaxForVoid(sale, voidDate)
            saleVoidHandlerForKafka.publishVoid(sale, saleVoidEntity)
        }
        saleRepository.save(sale)
        return saleAssembler.buildSummary(sale)
    }

    companion object {
        fun consolidationVoidReason(consolidatedSaleReferenceNumber: String): String =
            "Consolidated into $consolidatedSaleReferenceNumber"
    }
}
