package app.myliberty.easy_records.locations.business.retail.stock

import app.myliberty.easy_records.locations.business.core.lock.api.EntityAdvisoryLock
import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.MovementType
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class NetStockDrawReader(
    private val stockMovementRepository: StockMovementRepository,
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    fun lockAndReadNetDrawsOfDocument(documentReference: String, drawType: MovementType): List<NetStockDraw> =
        lockAndReadNetDraws(
            stockMovementRepository.findLocationProductIdsOfDocument(documentReference, drawType),
            documentReference,
            drawType
        ) { true }

    fun lockAndReadNetDrawsOfLines(
        documentReference: String,
        detailReferences: Collection<String>,
        drawType: MovementType
    ): List<NetStockDraw> = lockAndReadNetDraws(
        stockMovementRepository.findLocationProductIdsOfDocumentLines(documentReference, detailReferences, drawType),
        documentReference,
        drawType
    ) { it.detailReference in detailReferences }

     private fun lockAndReadNetDraws(
        locationProductIds: Set<UUID>,
        documentReference: String,
        drawType: MovementType,
        isInScope: (StockMovementEntity) -> Boolean
    ): List<NetStockDraw> {
        if (locationProductIds.isEmpty()) return emptyList()
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductIds)
        val stockMovementEntities = stockMovementRepository
            .findByDocumentReferenceAndMovementTypeInOrderByCreatedOn(documentReference, drawType.withReversalTypes())
            .filter(isInScope)
        return NetStockDraw.netOf(stockMovementEntities, drawType)
    }
}
