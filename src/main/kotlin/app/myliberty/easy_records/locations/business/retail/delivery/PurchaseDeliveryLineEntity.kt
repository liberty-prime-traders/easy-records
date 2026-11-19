package app.myliberty.easy_records.locations.business.retail.delivery

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.PurchaseDeliveryLineStatus
import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.PurchaseDeliveryLineStatusConverter
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.business.Decimals
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited
import java.math.BigDecimal
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.PURCHASE_DELIVERY_LINE)
@HasReference(tableName = TableName.PURCHASE_DELIVERY_LINE)
class PurchaseDeliveryLineEntity(

  @Column(name = "purchase_delivery_id", nullable = false)
  var purchaseDeliveryId: UUID,

  @Column(name = "purchase_line_id", nullable = false)
  var purchaseLineId: UUID,

  @Column(name = "user_provided_quantity_delivered", nullable = false, precision = 19, scale = 4)
  var userProvidedQuantityDelivered: BigDecimal,

  @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
  var unitCost: BigDecimal,

  @Column(name = "unit_id", nullable = false)
  var unitId: UUID,

  @Column(name = "storage_units_per_user_provided_unit", nullable = false, updatable = false)
  var storageUnitsPerUserProvidedUnit: Long,

  @Convert(converter = PurchaseDeliveryLineStatusConverter::class)
  @Column(name = "status", nullable = false, length = 5)
  var status: PurchaseDeliveryLineStatus = PurchaseDeliveryLineStatus.ACTIVE

) : HasReferenceEntity() {

  fun lineTotal(): BigDecimal = Decimals.MoneyScale.multiply(userProvidedQuantityDelivered, unitCost)

}
