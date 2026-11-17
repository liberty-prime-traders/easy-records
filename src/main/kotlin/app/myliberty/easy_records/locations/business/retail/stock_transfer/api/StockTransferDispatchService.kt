package app.myliberty.easy_records.locations.business.stock_transfer.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.location_product.api.ProductStockBreakdownProvider
import app.myliberty.easy_records.locations.business.lock.api.EntityAdvisoryLock
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import app.myliberty.easy_records.locations.business.stock.api.availability.StockAvailabilityValidator
import app.myliberty.easy_records.locations.business.stock.api.read.StockBalanceFetcher
import app.myliberty.easy_records.locations.business.stock.api.updater.StockTransferDispatchLineStockRequest
import app.myliberty.easy_records.locations.business.stock.api.updater.StockTransferStockUpdater
import app.myliberty.easy_records.locations.business.stock_transfer.ReconciledTransferLine
import app.myliberty.easy_records.locations.business.stock_transfer.StockTransferDispatchLineEntity
import app.myliberty.easy_records.locations.business.stock_transfer.StockTransferDispatchLineRepository
import app.myliberty.easy_records.locations.business.stock_transfer.StockTransferDispatchRepository
import app.myliberty.easy_records.locations.business.stock_transfer.StockTransferDraftDispatchFetcher
import app.myliberty.easy_records.locations.business.stock_transfer.StockTransferDispatchedHandlerForKafka
import app.myliberty.easy_records.locations.business.stock_transfer.StockTransferDraftLineRepository
import app.myliberty.easy_records.locations.business.stock_transfer.StockTransferFifoAllocator
import app.myliberty.easy_records.locations.business.stock_transfer.StockTransferOrderLocker
import app.myliberty.easy_records.locations.business.stock_transfer.StockTransferResponseAssembler
import app.myliberty.easy_records.locations.business.stock_transfer.StockTransferSchemaGateway
import app.myliberty.easy_records.locations.business.stock_transfer.StockTransferDraftLineEntity
import app.myliberty.easy_records.configuration.session.withLocationSchema
import app.myliberty.easy_records.locations.business.location_product.api.LocationProductSummaryDto
import app.myliberty.easy_records.organizations.business.location.api.LocationService
import app.myliberty.easy_records.organizations.business.unitconversion.api.UnitConversionGraphFacade
import app.myliberty.easy_records.util.quantity.BaseUnitDisplay
import app.myliberty.easy_records.util.quantity.ExactQuantity
import app.myliberty.easy_records.organizations.business.stock_transfer.api.StockTransferOrderService
import app.myliberty.easy_records.organizations.business.stock_transfer.api.StockTransferStatus
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.business.mappers.UserQualifier
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class StockTransferDispatchService(
    private val stockTransferOrderService: StockTransferOrderService,
    private val stockTransferDispatchRepository: StockTransferDispatchRepository,
    private val stockTransferDraftLineRepository: StockTransferDraftLineRepository,
    private val stockTransferDispatchLineRepository: StockTransferDispatchLineRepository,
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val stockTransferDispatchedHandlerForKafka: StockTransferDispatchedHandlerForKafka,
    private val stockTransferStockUpdater: StockTransferStockUpdater,
    private val stockTransferResponseAssembler: StockTransferResponseAssembler,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val locationService: LocationService,
    private val userQualifier: UserQualifier,
    private val stockTransferDraftDispatchFetcher: StockTransferDraftDispatchFetcher,
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val stockAvailabilityValidator: StockAvailabilityValidator,
    private val productStockBreakdownProvider: ProductStockBreakdownProvider,
    private val stockTransferOrderLocker: StockTransferOrderLocker,
    private val stockTransferSchemaGateway: StockTransferSchemaGateway,
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
) {

    @TransactionalOnLocationSchema
    fun dispatch(orderReferenceNumber: String): StockTransferResponse {
        val orderBeforeDispatch = stockTransferOrderLocker.lockThenFetch(orderReferenceNumber)
        val dispatchEntity = stockTransferDraftDispatchFetcher.requireDraftDispatch(orderReferenceNumber)
        val draftLines = stockTransferDraftLineRepository.findByStockTransferDispatchId(dispatchEntity.id!!)
        if (draftLines.isEmpty()) throw RtsGenericException("Cannot dispatch a transfer with no lines")

        val locationProductIds = draftLines.map { it.locationProductId }
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductIds.toSet())
        val fifoEntriesByProduct = stockBalanceFetcher.getFifoEntriesByProduct(locationProductIds)
        val orgProductIdByLocationProductId = locationProductDataFetcher.getOrgProductIds(locationProductIds)
        val summariesByLocationProductId = locationProductDataFetcher.findSummaryByIds(locationProductIds)
        val productLabelByLocationProductId = summariesByLocationProductId.mapValues { (_, summary) -> summary.label }

        val storageQuantityNeededByLocationProductId = draftLines.associate {
            it.locationProductId to it.storageQuantity()
        }
        stockAvailabilityValidator.guardSufficientStock(storageQuantityNeededByLocationProductId, productLabelByLocationProductId)
        guardDestinationCountsInSameStorageUnit(
            orderBeforeDispatch.destinationLocationId,
            draftLines,
            summariesByLocationProductId,
            orgProductIdByLocationProductId
        )

        val dispatchLines = StockTransferFifoAllocator.allocateDispatchLines(
            dispatchId = dispatchEntity.id!!,
            draftLines = draftLines,
            fifoEntriesByProduct = fifoEntriesByProduct,
            orgProductIdByLocationProductId = orgProductIdByLocationProductId,
            quantityBreakdownByLocationProductId = productStockBreakdownProvider.forLocationProducts(
                locationProductIds.toSet()
            ),
            productLabelByLocationProductId = productLabelByLocationProductId
        )

        stockTransferDraftLineRepository.deleteByStockTransferDispatchId(dispatchEntity.id!!)
        stockTransferDispatchLineRepository.saveAll(dispatchLines)

        stockTransferStockUpdater.consumeStockForDispatch(toStockRequests(dispatchEntity.requiredReference(), dispatchLines))

        val dispatchedAt = DateTimes.Offset.Now.organization()
        val dispatchedUserId = SessionContextProvider.getUserId()
        dispatchEntity.dispatchedById = dispatchedUserId
        dispatchEntity.dispatchedAt = dispatchedAt
        dispatchEntity.status = StockTransferStatus.DISPATCHED
        stockTransferDispatchRepository.save(dispatchEntity)

        val totalDispatchedCost = dispatchLines.sumOf { it.costDispatched }
        stockTransferOrderService.setDispatchSummary(
            referenceNumber = orderReferenceNumber,
            lineCount = dispatchLines.size,
            totalDispatchedCost = totalDispatchedCost,
            dispatchedAt = dispatchedAt,
            dispatchedByName = userQualifier.getUserFullName(dispatchedUserId)
        )

        val order = stockTransferOrderService.updateStatusToDispatched(orderReferenceNumber)
        stockTransferDispatchedHandlerForKafka.publish(
            orderId = order.id,
            orderReferenceNumber = orderReferenceNumber,
            dispatchReferenceNumber = dispatchEntity.requiredReference(),
            sourceSchema = SessionContextProvider.getLocationSchema(),
            totalCost = Money.of(totalDispatchedCost),
            postingDate = DateTimes.Local.atOrganizationZone(dispatchedAt)
        )
        return stockTransferResponseAssembler.buildDispatchOnly(
            orderDomainDto = order,
            dispatchEntity = dispatchEntity,
            lines = toReconciledLines(dispatchLines, summariesByLocationProductId)
        )
    }

    private fun toReconciledLines(
        dispatchLines: List<StockTransferDispatchLineEntity>,
        summariesByLocationProductId: Map<UUID, LocationProductSummaryDto>
    ): List<ReconciledTransferLine> {
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        val breakdownByLocationProductId = productStockBreakdownProvider.forLocationProducts(
            summariesByLocationProductId.keys
        )
        return dispatchLines.map { dispatchLine ->
            val summary = summariesByLocationProductId.getValue(dispatchLine.locationProductId)
            ReconciledTransferLine(
                dispatchLineReferenceNumber = dispatchLine.requiredReference(),
                productLabel = summary.label,
                quantity = dispatchLine.wholeLineUserProvidedQuantity,
                unitId = dispatchLine.unitId,
                baseUnitId = summary.baseUnitId,
                conversionFactor = BaseUnitDisplay.conversionFactor(
                    dispatchLine.storageUnitsPerUserProvidedUnit, unitConversionGraph.getSizeInRoot(summary.baseUnitId)
                ),
                unitCost = dispatchLine.unitCost,
                totalCost = dispatchLine.costDispatched,
                quantityReceived = null,
                storageQuantity = dispatchLine.storageQuantityDispatched,
                storageBreakdownText = breakdownByLocationProductId.getValue(dispatchLine.locationProductId)
                    .render(ExactQuantity.ofStorage(dispatchLine.storageQuantityDispatched))
            )
        }
    }

    private fun toStockRequests(
        dispatchReference: String,
        dispatchLines: List<StockTransferDispatchLineEntity>
    ): List<StockTransferDispatchLineStockRequest> =
        dispatchLines.map { dispatchLine ->
            StockTransferDispatchLineStockRequest(
                dispatchReference = dispatchReference,
                dispatchLineReference = dispatchLine.requiredReference(),
                locationProductId = dispatchLine.locationProductId,
                storageQuantity = ExactQuantity.ofStorage(dispatchLine.storageQuantityDispatched),
                userProvidedQuantity = dispatchLine.wholeLineUserProvidedQuantity,
                unitId = dispatchLine.unitId,
                storageUnitsPerUserProvidedUnit = dispatchLine.storageUnitsPerUserProvidedUnit,
            )
        }

    // The dispatched quantity is in the source's storage unit, so the destination must count stock in the same root; checked before any stock event
    private fun guardDestinationCountsInSameStorageUnit(
        destinationLocationId: UUID,
        draftLines: List<StockTransferDraftLineEntity>,
        summariesByLocationProductId: Map<UUID, LocationProductSummaryDto>,
        orgProductIdByLocationProductId: Map<UUID, UUID>
    ) {
        val destinationSchema = locationService.getSchemaByLocationId(destinationLocationId)
        val destinationBaseUnitIdByOrgProductId = locationService.withLocationSchema(destinationSchema) {
            stockTransferSchemaGateway.readBaseUnitIdsByOrgProductId(orgProductIdByLocationProductId.values)
        }
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        draftLines.forEach { draftLine ->
            val sourceStorageUnitId = unitConversionGraph.getStorageUnitId(summariesByLocationProductId.getValue(draftLine.locationProductId).baseUnitId)
            val orgProductId = orgProductIdByLocationProductId.getValue(draftLine.locationProductId)
            val destinationBaseUnitId = destinationBaseUnitIdByOrgProductId[orgProductId]
                ?: throw RtsGenericException("The destination location does not stock product $orgProductId")
            if (unitConversionGraph.getStorageUnitId(destinationBaseUnitId) != sourceStorageUnitId) {
                throw RtsGenericException(SOURCE_AND_DESTINATION_STORAGE_UNITS_DIFFER)
            }
        }
    }

    companion object {
        const val SOURCE_AND_DESTINATION_STORAGE_UNITS_DIFFER =
            "The source and destination products count stock in different storage units, so the transfer cannot be dispatched"
    }
}
