package app.myliberty.easy_records.locations.business.retail.stock_transfer.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.configuration.session.withLocationSchema
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.retail.stock.api.updater.StockTransferReceiptStockUpdater
import app.myliberty.easy_records.locations.business.retail.stock_transfer.toReceiptLineStockRequest
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferOrderLocker
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferReceiptCompletedHandlerForKafka
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferReceiptStatus
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferResponseAssembler
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferReceiptEntity
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferReceiptLineEntity
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferReceiptLineRepository
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferReceiptRepository
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferSchemaGateway
import app.myliberty.easy_records.organizations.business.core.location.api.LocationService
import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferOrderDomainDto
import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferOrderDataFetcher
import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferOrderService
import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferStatus
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class StockTransferReceiptService(
    private val stockTransferOrderService: StockTransferOrderService,
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher,
    private val stockTransferReceiptRepository: StockTransferReceiptRepository,
    private val stockTransferReceiptLineRepository: StockTransferReceiptLineRepository,
    private val stockTransferResponseAssembler: StockTransferResponseAssembler,
    private val stockTransferSchemaGateway: StockTransferSchemaGateway,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val stockTransferReceiptCompletedHandlerForKafka: StockTransferReceiptCompletedHandlerForKafka,
    private val stockTransferReceiptStockUpdater: StockTransferReceiptStockUpdater,
    private val stockTransferOrderLocker: StockTransferOrderLocker,
    private val locationService: LocationService
) {

    fun confirmLine(orderReferenceNumber: String, dispatchLineReferenceNumber: String): StockTransferResponse {
        val order = stockTransferOrderLocker.lockThenFetch(orderReferenceNumber)
        if (order.status != StockTransferStatus.DISPATCHED) {
            throw LibertyGenericException("Can only confirm lines on a DISPATCHED transfer. Current: ${order.status}")
        }

        val sourceSchema = locationService.getSchemaByLocationId(order.sourceLocationId)
        val dispatchLine = locationService.withLocationSchema(sourceSchema) { stockTransferSchemaGateway.readDispatchLine(dispatchLineReferenceNumber) }
        val destinationLocationProductId = locationProductDataFetcher
            .findIdentityByOrgProductId(dispatchLine.orgProductId).locationProductId

        val receipt = stockTransferReceiptRepository.findByStockTransferOrderRef(orderReferenceNumber)
            ?: stockTransferReceiptRepository.save(
                StockTransferReceiptEntity(
                    stockTransferOrderRef = orderReferenceNumber,
                    receivedById = SessionContextProvider.getUserId(),
                    receivedAt = DateTimes.Offset.Now.organization()
                )
            )

        if (receipt.status != StockTransferReceiptStatus.PENDING) {
            throw LibertyGenericException("Cannot confirm lines on a completed receipt")
        }

        if (!stockTransferReceiptLineRepository.existsByStockTransferDispatchLineRef(dispatchLineReferenceNumber)) {
            stockTransferReceiptLineRepository.save(
                StockTransferReceiptLineEntity(
                    stockTransferReceiptId = receipt.id!!,
                    stockTransferDispatchLineRef = dispatchLineReferenceNumber,
                    locationProductId = destinationLocationProductId,
                    storageQuantityReceived = dispatchLine.storageQuantityDispatched
                )
            )
        }

        return buildReceiptResponse(order, receipt)
    }

    fun unconfirmLine(orderReferenceNumber: String, dispatchLineReferenceNumber: String): StockTransferResponse {
        val order = stockTransferOrderLocker.lockThenFetch(orderReferenceNumber)
        val receipt = stockTransferReceiptRepository.findByStockTransferOrderRef(orderReferenceNumber)
            ?: throw LibertyGenericException("No receipt found for order $orderReferenceNumber")

        if (receipt.status != StockTransferReceiptStatus.PENDING) {
            throw LibertyGenericException("Cannot unconfirm lines on a completed receipt")
        }

        val receiptLine = stockTransferReceiptLineRepository
            .findByStockTransferDispatchLineRef(dispatchLineReferenceNumber)
            ?: throw LibertyGenericException("Receipt line for dispatch line $dispatchLineReferenceNumber not found")

        stockTransferReceiptLineRepository.delete(receiptLine)
        return buildReceiptResponse(order, receipt)
    }

    fun completeTransfer(receiptReferenceNumber: String): StockTransferResponse {
        val orderReferenceNumber = stockTransferReceiptRepository
            .findStockTransferOrderRefByReferenceNumber(receiptReferenceNumber)
            ?: throw LibertyGenericException("Receipt $receiptReferenceNumber not found")
        val order = stockTransferOrderLocker.lockThenFetch(orderReferenceNumber)
        // A COMPLETED order with a PENDING receipt is a completion whose credit never committed: the order and the dispatch
        // commit before the credit does. The credit is idempotent, so running it again finishes the transfer.
        if (order.status != StockTransferStatus.DISPATCHED && order.status != StockTransferStatus.COMPLETED) {
            throw LibertyGenericException("Can only complete a DISPATCHED transfer. Current: ${order.status}")
        }
        val receipt = stockTransferReceiptRepository.findByReferenceNumber(receiptReferenceNumber)
            ?: throw LibertyGenericException("Receipt $receiptReferenceNumber not found")
        if (receipt.status != StockTransferReceiptStatus.PENDING) {
            throw LibertyGenericException("Receipt is already ${receipt.status}")
        }
        val sourceSchema = locationService.getSchemaByLocationId(order.sourceLocationId)

        val dispatchWithLines = locationService.withLocationSchema(sourceSchema) {
            stockTransferSchemaGateway.readDispatchAndLines(orderReferenceNumber)
        }
        if (dispatchWithLines.dispatch.status == StockTransferStatus.CANCELLED) {
            throw LibertyGenericException(
                "The dispatch of transfer $orderReferenceNumber was cancelled, so it cannot be completed"
            )
        }
        val allDispatchLines = dispatchWithLines.lines
        val receiptLines = stockTransferReceiptLineRepository.findByStockTransferReceiptId(receipt.id!!)
        val confirmedDispatchLineReferenceNumbers = receiptLines.map { it.stockTransferDispatchLineRef }.toSet()
        val unconfirmedCount = allDispatchLines.count {
            it.requiredReference() !in confirmedDispatchLineReferenceNumbers
        }
        if (unconfirmedCount > 0) {
            throw LibertyGenericException("Cannot complete transfer #$orderReferenceNumber: $unconfirmedCount dispatch line(s) not yet confirmed")
        }

        stockTransferReceiptStockUpdater.recordTransferReceipt(
            dispatchReferenceNumber = dispatchWithLines.dispatch.requiredReference(),
            stockTransferReceiptLineStockRequests = allDispatchLines.map { it.toReceiptLineStockRequest() }
        )

        receipt.status = StockTransferReceiptStatus.COMPLETED
        stockTransferReceiptRepository.save(receipt)

        locationService.withLocationSchema(sourceSchema) {
            stockTransferSchemaGateway.markDispatchCompleted(orderReferenceNumber)
        }
        stockTransferOrderService.updateStatusToCompleted(orderReferenceNumber)

        stockTransferReceiptCompletedHandlerForKafka.publish(
            receiptId = receipt.id!!,
            orderReferenceNumber = orderReferenceNumber,
            dispatchReferenceNumber = dispatchWithLines.dispatch.requiredReference(),
            receiptReferenceNumber = receipt.requiredReference(),
            totalCost = Money.of(order.requiredTotalDispatchedCost())
        )

        return buildReceiptResponse(stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber), receipt)
    }

    private fun buildReceiptResponse(
        order: StockTransferOrderDomainDto,
        receipt: StockTransferReceiptEntity
    ): StockTransferResponse {
        val receiptLines = stockTransferReceiptLineRepository.findByStockTransferReceiptId(receipt.id!!)
        return stockTransferResponseAssembler.buildDispatchAndReceipt(order, receipt, receiptLines)
    }
}
