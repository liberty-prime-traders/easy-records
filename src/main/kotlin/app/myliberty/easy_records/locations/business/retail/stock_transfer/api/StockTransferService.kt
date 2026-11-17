package app.myliberty.easy_records.locations.business.retail.stock_transfer.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.configuration.session.withLocationSchema
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferCancelledHandlerForKafka
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferDispatchEntity
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferDispatchRepository
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferOrderLocker
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferResponseAssembler
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferSchemaGateway
import app.myliberty.easy_records.organizations.business.location.api.LocationService
import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferOrderDataFetcher
import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferOrderService
import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferStatus
import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class StockTransferService(
    private val stockTransferOrderService: StockTransferOrderService,
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher,
    private val stockTransferDispatchRepository: StockTransferDispatchRepository,
    private val stockTransferSchemaGateway: StockTransferSchemaGateway,
    private val stockTransferResponseAssembler: StockTransferResponseAssembler,
    private val stockTransferCancelledHandlerForKafka: StockTransferCancelledHandlerForKafka,
    private val stockTransferOrderLocker: StockTransferOrderLocker,
    private val locationService: LocationService
) {

    fun createTransfer(stockTransferCreateDto: StockTransferCreateDto): StockTransferResponse {
        val order = stockTransferOrderService.createOrder(
            sourceLocationId = SessionContextProvider.getLocationId(),
            destinationLocationId = stockTransferCreateDto.destinationLocationId,
            notes = stockTransferCreateDto.notes
        )
        val dispatchEntity = stockTransferDispatchRepository.save(StockTransferDispatchEntity(order.referenceNumber))
        return stockTransferResponseAssembler.buildDispatchOnly(order, dispatchEntity)
    }

    fun cancelTransfer(orderReferenceNumber: String): StockTransferResponse {
        val order = stockTransferOrderLocker.lockThenFetch(orderReferenceNumber)
        if (order.status != StockTransferStatus.DRAFT && order.status != StockTransferStatus.DISPATCHED) {
            throw RtsGenericException("Cannot cancel a transfer in status ${order.status}")
        }
        val sourceSchema = locationService.getSchemaByLocationId(order.sourceLocationId)
        val wasDispatched = order.status == StockTransferStatus.DISPATCHED
        val dispatchReferenceNumber = locationService.withLocationSchema(sourceSchema) {
            stockTransferSchemaGateway.cancelDispatch(orderReferenceNumber)
        }
        stockTransferOrderService.updateStatusToCancelled(orderReferenceNumber)
        if (wasDispatched) {
            stockTransferCancelledHandlerForKafka.publish(
                orderId = order.id,
                orderReferenceNumber = orderReferenceNumber,
                dispatchReferenceNumber = dispatchReferenceNumber,
                sourceSchema = sourceSchema,
                totalCost = Money.of(order.requiredTotalDispatchedCost())
            )
        }
        val cancelledOrder = stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber)
        return stockTransferResponseAssembler.build(cancelledOrder)
    }

    fun fetchTop(limit: Int): List<StockTransferResponse> {
        return stockTransferOrderDataFetcher.getTopByLocationId(limit)
            .map { order -> stockTransferResponseAssembler.build(order) }
    }

    fun fetchByOrderReferenceNumber(orderReferenceNumber: String): StockTransferResponse {
        val order = stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber)
        return stockTransferResponseAssembler.build(order)
    }
}
