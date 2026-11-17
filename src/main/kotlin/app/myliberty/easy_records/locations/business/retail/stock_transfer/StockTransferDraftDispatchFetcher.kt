package app.myliberty.easy_records.locations.business.retail.stock_transfer

import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferStatus
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Component

@Component
class StockTransferDraftDispatchFetcher(
    private val stockTransferDispatchRepository: StockTransferDispatchRepository
) {

    fun requireDraftDispatch(orderReferenceNumber: String): StockTransferDispatchEntity {
        val dispatchEntity = stockTransferDispatchRepository.findByStockTransferOrderRef(orderReferenceNumber)
            ?: throw LibertyGenericException("Dispatch not found for order $orderReferenceNumber")
        if (dispatchEntity.status != StockTransferStatus.DRAFT) {
            throw LibertyGenericException("Operation only allowed in DRAFT status. Current: ${dispatchEntity.status}")
        }
        return dispatchEntity
    }
}
