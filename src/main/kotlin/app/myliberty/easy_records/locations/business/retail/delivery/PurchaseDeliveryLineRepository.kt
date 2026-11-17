package app.myliberty.easy_records.locations.business.retail.delivery

import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.PurchaseDeliveryLineStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PurchaseDeliveryLineRepository : JpaRepository<PurchaseDeliveryLineEntity, UUID> {
  fun findByReferenceNumberIn(referenceNumbers: List<String>): List<PurchaseDeliveryLineEntity>

  fun findByPurchaseDeliveryIdIn(deliveryIds: List<UUID>): List<PurchaseDeliveryLineEntity>

  fun findByPurchaseDeliveryIdInAndStatus(
    deliveryIds: Collection<UUID>,
    status: PurchaseDeliveryLineStatus
  ): List<PurchaseDeliveryLineEntity>

  fun findByPurchaseLineIdAndStatus(
    purchaseLineId: UUID,
    status: PurchaseDeliveryLineStatus
  ): List<PurchaseDeliveryLineEntity>

  @Query(
    "SELECT d.purchaseId AS purchaseId, l.purchaseLineId AS purchaseLineId " +
      "FROM PurchaseDeliveryEntity d, PurchaseDeliveryLineEntity l " +
      "WHERE l.id = :deliveryLineId AND d.id = l.purchaseDeliveryId"
  )
  fun findOwnersById(deliveryLineId: UUID): PurchaseDeliveryLineOwners?
}
