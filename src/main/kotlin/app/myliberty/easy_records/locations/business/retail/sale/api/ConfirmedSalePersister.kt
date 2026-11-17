package app.myliberty.easy_records.locations.business.sale.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.sale.SaleConfirmedHandlerForKafka
import app.myliberty.easy_records.locations.business.sale.SaleEntity
import app.myliberty.easy_records.locations.business.sale.SaleLineEntity
import app.myliberty.easy_records.locations.business.sale.SaleLineCogsEntity
import app.myliberty.easy_records.locations.business.sale.SaleLineCogsRepository
import app.myliberty.easy_records.locations.business.sale.SaleLineRepository
import app.myliberty.easy_records.locations.business.sale.SaleLocker
import app.myliberty.easy_records.locations.business.sale.SaleLineSync
import app.myliberty.easy_records.locations.business.sale.SaleRepository
import app.myliberty.easy_records.locations.business.sale.SaleSaveFinalizer
import app.myliberty.easy_records.locations.business.sale.SaleTaxFinalizer
import app.myliberty.easy_records.locations.business.sale.SaleValidator
import app.myliberty.easy_records.locations.business.sale_adjustment.api.SaleAdjustmentSyncer
import app.myliberty.easy_records.locations.business.stock.api.updater.SaleLineStockRequest
import app.myliberty.easy_records.locations.business.stock.api.updater.SaleStockUpdater
import app.myliberty.easy_records.locations.business.stock_reservation.api.StockReserver
import app.myliberty.easy_records.organizations.business.fiscal_period.api.FiscalPeriodService
import app.myliberty.easy_records.util.business.DateTimes
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class ConfirmedSalePersister(
    private val saleRepository: SaleRepository,
    private val saleLineRepository: SaleLineRepository,
    private val saleLineCogsRepository: SaleLineCogsRepository,
    private val stockReserver: StockReserver,
    private val saleStockUpdater: SaleStockUpdater,
    private val saleConfirmedHandlerForKafka: SaleConfirmedHandlerForKafka,
    private val saleLocker: SaleLocker,
    private val fiscalPeriodService: FiscalPeriodService,
    private val saleSaveFinalizer: SaleSaveFinalizer,
    private val saleAdjustmentSyncer: SaleAdjustmentSyncer,
    private val saleValidator: SaleValidator,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val saleTaxFinalizer: SaleTaxFinalizer,
) {

    fun confirm(saleSaveRequest: SaleSaveRequest): SaleSaveResult {
        SaleValidator.guardNoPreAllocatedReferenceOnConfirm(saleSaveRequest)
        val effectiveDateSold = saleSaveRequest.dateSold ?: DateTimes.Offset.Now.organization()
        SaleValidator.guardDateSoldIsNotFuture(effectiveDateSold)
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.atOrganizationZone(effectiveDateSold))

        val saleEntity = loadOrCreate(saleSaveRequest, effectiveDateSold)
        saleEntity.contactId = saleSaveRequest.contactId
        saleEntity.soldById = saleSaveRequest.soldById ?: SessionContextProvider.getUserId()
        saleEntity.dateSold = effectiveDateSold
        saleEntity.notes = saleSaveRequest.notes
        saleRepository.save(saleEntity)

        stockReserver.clearBySale(saleEntity.id!!)
        val retainedSaleAdjustments = saleAdjustmentSyncer.deleteRemoved(saleEntity.id!!, saleSaveRequest)
        val lineSyncResult = SaleLineSync.sync(saleEntity, saleSaveRequest, saleLineRepository)
        guardStockAvailability(saleEntity.id!!, lineSyncResult.persistedSaleLines)

        saleEntity.status = SaleStatus.CONFIRMED
        val saleUpdateResult = saleSaveFinalizer.finalize(
            saleEntity = saleEntity,
            saleSaveRequest = saleSaveRequest,
            lineSyncResult = lineSyncResult,
            retainedSaleAdjustments = retainedSaleAdjustments,
        )

        consumeStockAndRecordCogs(saleEntity, lineSyncResult.persistedSaleLines)
        saleTaxFinalizer.finalizeTaxForConfirm(saleEntity)
        saleConfirmedHandlerForKafka.publish(saleEntity)

        return SaleSaveResult(
            saleId = saleEntity.id!!,
            saleReferenceNumber = saleEntity.requiredReference(),
            newVersion = saleEntity.version,
            saleStatus = saleEntity.status,
            dateSold = saleEntity.dateSold,
            soldById = saleEntity.soldById,
            saleLineIdsByClientKey = lineSyncResult.saleLineIdsByClientKey,
            saleAdjustmentIdsByClientKey = saleUpdateResult.saleAdjustmentIdsByClientKey,
            persistedSalePaymentsByClientKey = saleUpdateResult.persistedSalePaymentsByClientKey,
        )
    }

    private fun guardStockAvailability(saleId: UUID, saleLineEntities: List<SaleLineEntity>) {
        if (saleLineEntities.isEmpty()) return
        val storageQuantitiesByLocationProductId = saleLineEntities.associate { it.locationProductId to it.storageQuantity() }
        val productSummariesByLocationProductId = locationProductDataFetcher
            .findSummaryByIds(storageQuantitiesByLocationProductId.keys)
        saleValidator.guardSufficientStockForSale(saleId, storageQuantitiesByLocationProductId, productSummariesByLocationProductId)
    }

    private fun consumeStockAndRecordCogs(
        saleEntity: SaleEntity,
        saleLineEntities: List<SaleLineEntity>,
    ) {
        val saleLineStockRequests = saleLineEntities.map { saleLineEntity ->
            SaleLineStockRequest(
                saleLineId = saleLineEntity.id!!,
                saleLineReferenceNumber = saleLineEntity.requiredReference(),
                locationProductId = saleLineEntity.locationProductId,
                userProvidedQuantity = saleLineEntity.userProvidedQuantity,
                unitId = saleLineEntity.unitId,
                storageUnitsPerUserProvidedUnit = saleLineEntity.storageUnitsPerUserProvidedUnit,
            )
        }

        val consumedCostsBySaleLineId = saleStockUpdater.consumeStock(
            saleLineStockRequests,
            saleEntity.requiredReference()
        ).associateBy { it.saleLineId }
        saleLineEntities.forEach { saleLineEntity ->
            saleLineEntity.cogsTotal = consumedCostsBySaleLineId.getValue(saleLineEntity.id!!).cost
        }
        saleLineRepository.saveAll(saleLineEntities)
        saleLineCogsRepository.saveAll(saleLineEntities.map { saleLineEntity ->
            SaleLineCogsEntity(saleLineId = saleLineEntity.id!!, cogsTotal = saleLineEntity.cogsTotal!!)
        })
    }

    private fun loadOrCreate(saleSaveRequest: SaleSaveRequest, effectiveDateSold: OffsetDateTime): SaleEntity {
        val saleId = saleSaveRequest.saleId ?: return SaleEntity(
            contactId = saleSaveRequest.contactId,
            soldById = saleSaveRequest.soldById,
            dateSold = effectiveDateSold,
            notes = saleSaveRequest.notes,
            status = SaleStatus.CONFIRMED,
        )
        return saleLocker.loadDraftAtVersion(saleId, saleSaveRequest.expectedVersion)
    }
}
