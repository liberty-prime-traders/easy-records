package app.myliberty.easy_records.locations.business.retail.stock_transfer.api

import app.myliberty.easy_records.configuration.session.LocationSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.core.lock.api.EntityAdvisoryLock
import app.myliberty.easy_records.locations.business.retail.stock.api.updater.StockTransferReceiptLineStockRequest
import app.myliberty.easy_records.locations.business.retail.stock.api.updater.StockTransferReceiptStockUpdater
import app.myliberty.easy_records.locations.business.retail.stock_transfer.DispatchWithLines
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferDispatchEntity
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferDispatchLineEntity
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferReceiptCompletedHandlerForKafka
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferReceiptEntity
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferReceiptLineEntity
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferReceiptLineRepository
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferReceiptRepository
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferReceiptStatus
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferOrderLocker
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferResponseAssembler
import app.myliberty.easy_records.locations.business.retail.stock_transfer.StockTransferSchemaGateway
import app.myliberty.easy_records.organizations.business.core.location.api.LocationService
import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferOrderDataFetcher
import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferOrderDomainDto
import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferOrderService
import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferStatus
import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class StockTransferReceiptServiceTest {

    private val orderId = UUID.randomUUID()
    private val sourceLocationId = UUID.randomUUID()
    private val receiptId = UUID.randomUUID()
    private val orgProductId = UUID.randomUUID()
    private val destinationLocationProductId = UUID.randomUUID()
    private val unitId = UUID.randomUUID()
    private val orderReferenceNumber = "ORD-1"
    private val receiptReferenceNumber = "RCP-1"
    private val dispatchReferenceNumber = "DISPATCH-1"
    private val sourceSchema = "loc-source"

    private val stockTransferOrderService = mock(StockTransferOrderService::class.java)
    private val stockTransferOrderDataFetcher = mock(StockTransferOrderDataFetcher::class.java)
    private val stockTransferReceiptRepository = mock(StockTransferReceiptRepository::class.java)
    private val stockTransferReceiptLineRepository = mock(StockTransferReceiptLineRepository::class.java)
    private val stockTransferResponseAssembler = mock(StockTransferResponseAssembler::class.java)
    private val stockTransferSchemaGateway = mock(StockTransferSchemaGateway::class.java)
    private val stockTransferReceiptStockUpdater = mock(StockTransferReceiptStockUpdater::class.java)
    private val stockTransferReceiptCompletedHandlerForKafka =
        mock(StockTransferReceiptCompletedHandlerForKafka::class.java)
    private val entityAdvisoryLock = mock(EntityAdvisoryLock::class.java)
    private val stockTransferOrderLocker = StockTransferOrderLocker(stockTransferOrderDataFetcher, entityAdvisoryLock)
    private val locationService = mock(LocationService::class.java)
    private val stockTransferReceiptService = StockTransferReceiptService(
        stockTransferOrderService = stockTransferOrderService,
        stockTransferOrderDataFetcher = stockTransferOrderDataFetcher,
        stockTransferReceiptRepository = stockTransferReceiptRepository,
        stockTransferReceiptLineRepository = stockTransferReceiptLineRepository,
        stockTransferResponseAssembler = stockTransferResponseAssembler,
        stockTransferSchemaGateway = stockTransferSchemaGateway,
        locationProductDataFetcher = mock(LocationProductDataFetcher::class.java),
        stockTransferReceiptCompletedHandlerForKafka = stockTransferReceiptCompletedHandlerForKafka,
        stockTransferReceiptStockUpdater = stockTransferReceiptStockUpdater,
        stockTransferOrderLocker = stockTransferOrderLocker,
        locationService = locationService
    )

    @BeforeEach
    fun setUpDestinationSession() {
        SessionContextProvider.setSession(
            SessionContext(location = LocationSession(UUID.randomUUID(), "loc-destination", "UTC"))
        )
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    private fun order(status: StockTransferStatus) = StockTransferOrderDomainDto(
        id = orderId,
        referenceNumber = orderReferenceNumber,
        sourceLocationId = sourceLocationId,
        destinationLocationId = UUID.randomUUID(),
        status = status,
        notes = null,
        lineCount = 1,
        totalDispatchedCost = BigDecimal("10.0000"),
        dispatchedAt = OffsetDateTime.now(),
        dispatchedByName = null,
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now()
    )

    private fun receipt(status: StockTransferReceiptStatus) = StockTransferReceiptEntity(
        stockTransferOrderRef = orderReferenceNumber,
        receivedById = UUID.randomUUID(),
        receivedAt = OffsetDateTime.now(),
        status = status
    ).also {
        it.id = receiptId
        it.referenceNumber = receiptReferenceNumber
    }

    private fun dispatchLine() = StockTransferDispatchLineEntity(
        stockTransferDispatchId = UUID.randomUUID(),
        orgProductId = orgProductId,
        locationProductId = UUID.randomUUID(),
        wholeLineUserProvidedQuantity = BigDecimal("2.0000"),
        storageQuantityDispatched = BigDecimal("36.0000"),
        unitId = unitId,
        unitCost = BigDecimal("2.7777777778"),
        costDispatched = BigDecimal("10.0000"),
        storageUnitsPerUserProvidedUnit = 18L
    ).also { it.referenceNumber = "DL-1" }

    private fun expectedReceiptLineRequest() = StockTransferReceiptLineStockRequest(
        dispatchLineReferenceNumber = "DL-1",
        orgProductId = orgProductId,
        wholeLineUserProvidedQuantity = BigDecimal("2.0000"),
        storageQuantityDispatched = BigDecimal("36.0000"),
        costDispatched = BigDecimal("10.0000"),
        unitId = unitId,
        storageUnitsPerUserProvidedUnit = 18L
    )

    private fun givenReceiptBelongsToOrder() {
        `when`(stockTransferReceiptRepository.findStockTransferOrderRefByReferenceNumber(receiptReferenceNumber))
            .thenReturn(orderReferenceNumber)
    }

    private fun givenOrderWithOneDispatchLine(
        orderStatus: StockTransferStatus = StockTransferStatus.DISPATCHED,
        dispatchStatus: StockTransferStatus = StockTransferStatus.DISPATCHED
    ) {
        givenReceiptBelongsToOrder()
        `when`(stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber))
            .thenReturn(order(orderStatus))
        `when`(locationService.getSchemaByLocationId(sourceLocationId)).thenReturn(sourceSchema)
        `when`(stockTransferSchemaGateway.readDispatchAndLines(orderReferenceNumber)).thenReturn(
            DispatchWithLines(
                StockTransferDispatchEntity(orderReferenceNumber, dispatchStatus)
                    .also { it.referenceNumber = dispatchReferenceNumber },
                listOf(dispatchLine())
            )
        )
        `when`(stockTransferReceiptRepository.findByReferenceNumber(receiptReferenceNumber))
            .thenReturn(receipt(StockTransferReceiptStatus.PENDING))
    }

    private fun givenReceiptLinesConfirmed(vararg dispatchLineReferences: String) {
        `when`(stockTransferReceiptLineRepository.findByStockTransferReceiptId(receiptId)).thenReturn(
            dispatchLineReferences.map {
                StockTransferReceiptLineEntity(receiptId, it, destinationLocationProductId, BigDecimal("36.0000"))
            }
        )
    }

    @Test
    fun `completing a fully confirmed receipt credits the destination, completes both sides and publishes the event`() {
        givenOrderWithOneDispatchLine()
        givenReceiptLinesConfirmed("DL-1")

        stockTransferReceiptService.completeTransfer(receiptReferenceNumber)

        val inOrder = inOrder(
            entityAdvisoryLock, stockTransferReceiptStockUpdater, stockTransferReceiptRepository,
            stockTransferSchemaGateway, stockTransferOrderService, stockTransferReceiptCompletedHandlerForKafka
        )
        inOrder.verify(entityAdvisoryLock).acquire(LockNamespaces.STOCK_TRANSFER_ORDER, orderId)
        inOrder.verify(stockTransferReceiptStockUpdater)
            .recordTransferReceipt(dispatchReferenceNumber, listOf(expectedReceiptLineRequest()))
        inOrder.verify(stockTransferReceiptRepository).save(
            argThat<StockTransferReceiptEntity> { it.status == StockTransferReceiptStatus.COMPLETED }
        )
        inOrder.verify(stockTransferSchemaGateway).markDispatchCompleted(orderReferenceNumber)
        inOrder.verify(stockTransferOrderService).updateStatusToCompleted(orderReferenceNumber)
        inOrder.verify(stockTransferReceiptCompletedHandlerForKafka).publish(
            receiptId, orderReferenceNumber, dispatchReferenceNumber, receiptReferenceNumber,
            Money.of(BigDecimal("10.0000"))
        )
    }

    @Test
    fun `a transfer whose order and dispatch completed but whose credit never committed is credited on retry`() {
        givenOrderWithOneDispatchLine(StockTransferStatus.COMPLETED, StockTransferStatus.COMPLETED)
        givenReceiptLinesConfirmed("DL-1")

        stockTransferReceiptService.completeTransfer(receiptReferenceNumber)

        verify(stockTransferReceiptStockUpdater)
            .recordTransferReceipt(dispatchReferenceNumber, listOf(expectedReceiptLineRequest()))
        verify(stockTransferReceiptCompletedHandlerForKafka).publish(
            receiptId, orderReferenceNumber, dispatchReferenceNumber, receiptReferenceNumber,
            Money.of(BigDecimal("10.0000"))
        )
    }

    @Test
    fun `a dispatch cancelled under a still-dispatched order refuses completion and credits nothing`() {
        givenOrderWithOneDispatchLine(StockTransferStatus.DISPATCHED, StockTransferStatus.CANCELLED)
        givenReceiptLinesConfirmed("DL-1")

        assertThrows(RtsGenericException::class.java) {
            stockTransferReceiptService.completeTransfer(receiptReferenceNumber)
        }

        verifyNoInteractions(stockTransferReceiptStockUpdater, stockTransferReceiptCompletedHandlerForKafka)
    }

    @Test
    fun `a transfer that is completed on every side cannot be completed twice`() {
        givenOrderWithOneDispatchLine(StockTransferStatus.COMPLETED, StockTransferStatus.COMPLETED)
        `when`(stockTransferReceiptRepository.findByReferenceNumber(receiptReferenceNumber))
            .thenReturn(receipt(StockTransferReceiptStatus.COMPLETED))

        assertThrows(RtsGenericException::class.java) {
            stockTransferReceiptService.completeTransfer(receiptReferenceNumber)
        }

        verifyNoInteractions(stockTransferReceiptStockUpdater, stockTransferReceiptCompletedHandlerForKafka)
    }

    @Test
    fun `completing a receipt with an unconfirmed dispatch line throws and credits nothing`() {
        givenOrderWithOneDispatchLine()
        givenReceiptLinesConfirmed()

        assertThrows(RtsGenericException::class.java) {
            stockTransferReceiptService.completeTransfer(receiptReferenceNumber)
        }

        verifyNoInteractions(stockTransferReceiptStockUpdater, stockTransferReceiptCompletedHandlerForKafka)
    }

    @Test
    fun `completing a transfer that was cancelled while waiting for the order lock throws and credits nothing`() {
        givenReceiptBelongsToOrder()
        `when`(stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber))
            .thenReturn(order(StockTransferStatus.DISPATCHED), order(StockTransferStatus.CANCELLED))

        assertThrows(RtsGenericException::class.java) {
            stockTransferReceiptService.completeTransfer(receiptReferenceNumber)
        }

        verifyNoInteractions(stockTransferReceiptStockUpdater, stockTransferReceiptCompletedHandlerForKafka)
    }

    @Test
    fun `the order status is read again after the order lock is taken`() {
        givenReceiptBelongsToOrder()
        `when`(stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber))
            .thenReturn(order(StockTransferStatus.DISPATCHED), order(StockTransferStatus.CANCELLED))

        assertThrows(RtsGenericException::class.java) {
            stockTransferReceiptService.completeTransfer(receiptReferenceNumber)
        }

        val inOrder = inOrder(stockTransferOrderDataFetcher, entityAdvisoryLock)
        inOrder.verify(stockTransferOrderDataFetcher).getByReferenceNumber(orderReferenceNumber)
        inOrder.verify(entityAdvisoryLock).acquire(LockNamespaces.STOCK_TRANSFER_ORDER, orderId)
        inOrder.verify(stockTransferOrderDataFetcher).getByReferenceNumber(orderReferenceNumber)
    }

    @Test
    fun `completing a receipt that is already completed throws and credits nothing`() {
        givenReceiptBelongsToOrder()
        `when`(stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber))
            .thenReturn(order(StockTransferStatus.DISPATCHED))
        `when`(stockTransferReceiptRepository.findByReferenceNumber(receiptReferenceNumber))
            .thenReturn(receipt(StockTransferReceiptStatus.COMPLETED))

        assertThrows(RtsGenericException::class.java) {
            stockTransferReceiptService.completeTransfer(receiptReferenceNumber)
        }

        verifyNoInteractions(stockTransferReceiptStockUpdater, stockTransferReceiptCompletedHandlerForKafka)
    }

    @Test
    fun `completing an unknown receipt throws before any lock is taken`() {
        assertThrows(RtsGenericException::class.java) {
            stockTransferReceiptService.completeTransfer(receiptReferenceNumber)
        }

        verifyNoInteractions(entityAdvisoryLock)
    }
}
