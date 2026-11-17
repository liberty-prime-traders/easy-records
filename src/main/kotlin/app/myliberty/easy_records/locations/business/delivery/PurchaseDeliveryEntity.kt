package app.myliberty.easy_records.locations.business.delivery

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.locations.business.purchase.api.delivery.PurchaseDeliveryStatusConverter
import app.myliberty.easy_records.locations.business.purchase.api.delivery.PurchaseDeliveryStatus
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited
import java.time.OffsetDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.PURCHASE_DELIVERY)
@HasReference(tableName = TableName.PURCHASE_DELIVERY)
class PurchaseDeliveryEntity(

  @Column(name = "purchase_id", nullable = false)
  var purchaseId: UUID,

  @Convert(converter = PurchaseDeliveryStatusConverter::class)
  @Column(name = "status", nullable = false, length = 5)
  var status: PurchaseDeliveryStatus = PurchaseDeliveryStatus.PROCESSING,

  @Column(name = "delivered_at")
  var deliveredAt: OffsetDateTime,

  @Column(name = "notes")
  var notes: String? = null

) : HasReferenceEntity()
