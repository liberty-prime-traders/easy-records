package app.myliberty.easy_records.locations.business.stock_transfer

import app.myliberty.easy_records.organizations.business.stock_transfer.api.StockTransferStatus
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
class StockTransferDraftDispatchFetcher(
    private val stockTransferDispatchRepository: StockTransferDispatchRepository
) {

    fun requireDraftDispatch(orderReferenceNumber: String): StockTransferDispatchEntity {
        val dispatchEntity = stockTransferDispatchRepository.findByStockTransferOrderRef(orderReferenceNumber)
            ?: throw RtsGenericException("Dispatch not found for order $orderReferenceNumber")
        if (dispatchEntity.status != StockTransferStatus.DRAFT) {
            throw RtsGenericException("Operation only allowed in DRAFT status. Current: ${dispatchEntity.status}")
        }
        return dispatchEntity
    }
}
