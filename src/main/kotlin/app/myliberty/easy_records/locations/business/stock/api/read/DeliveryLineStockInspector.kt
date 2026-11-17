package app.myliberty.easy_records.locations.business.stock.api.read

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.stock.StockEntryRepository
import app.myliberty.easy_records.locations.business.stock.StockMovementRepository
import app.myliberty.easy_records.organizations.business.stock_movement_reason.api.MovementType
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class DeliveryLineStockInspector(
    private val stockMovementRepository: StockMovementRepository,
    private val stockEntryRepository: StockEntryRepository
) {

    fun getStockEntryId(deliveryReferenceNumber: String, deliveryLineReferenceNumber: String): UUID =
        stockEntryRepository.findIdsByDocumentAndDetailReference(
            deliveryReferenceNumber,
            deliveryLineReferenceNumber
        ).singleOrNull()
            ?: throw RtsGenericException(
                "The stock records of delivery line $deliveryLineReferenceNumber are incomplete, " +
                    "so it cannot be changed. Please contact support."
            )

    fun hasTransferredOut(deliveryReferenceNumber: String, deliveryLineReferenceNumber: String): Boolean =
        stockMovementRepository.countNetTransferredOutOnEntriesOfSource(
            deliveryReferenceNumber,
            deliveryLineReferenceNumber,
            MovementType.TRANSFER_OUT,
            MovementType.TRANSFER_CANCELLED
        ) > 0

    fun hasMovementsThatCannotBeReversed(
        deliveryReferenceNumber: String,
        deliveryLineReferenceNumber: String
    ): Boolean =
        stockMovementRepository.countMovementsOfTypesOnEntriesOfSource(
            deliveryReferenceNumber,
            deliveryLineReferenceNumber,
            MovementType.movementsThatCannotBeReversed
        ) > 0
}
