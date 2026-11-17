package app.myliberty.easy_records.locations.business.delivery

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PurchaseDeliveryRepository : JpaRepository<PurchaseDeliveryEntity, UUID> {
  fun findByPurchaseIdIn(purchaseIds: Collection<UUID>): List<PurchaseDeliveryEntity>
}
