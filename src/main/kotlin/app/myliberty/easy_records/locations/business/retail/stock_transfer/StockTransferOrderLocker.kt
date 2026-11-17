package app.myliberty.easy_records.locations.business.retail.stock_transfer

import app.myliberty.easy_records.locations.business.lock.api.EntityAdvisoryLock
import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferOrderDataFetcher
import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferOrderDomainDto
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import org.springframework.stereotype.Component

@Component
class StockTransferOrderLocker(
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher,
    private val entityAdvisoryLock: EntityAdvisoryLock
) {

    // A status read before the lock can be stale behind a concurrent dispatch, cancel or completion, so the caller
    // decides on the order this returns. Needs an enclosing location transaction, as the lock does.
    fun lockThenFetch(orderReferenceNumber: String): StockTransferOrderDomainDto {
        val orderId = stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber).id
        entityAdvisoryLock.acquire(LockNamespaces.STOCK_TRANSFER_ORDER, orderId)
        return stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber)
    }
}
