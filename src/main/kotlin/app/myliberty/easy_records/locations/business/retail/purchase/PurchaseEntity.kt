package app.myliberty.easy_records.locations.business.retail.purchase

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.enums.PaymentStatus
import app.myliberty.easy_records.util.enums.PaymentStatusConverter
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseStatus
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.ColumnNames
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited
import java.time.OffsetDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.PURCHASE)
@HasReference(tableName = TableName.PURCHASE)
class PurchaseEntity(

  @Column(name = ColumnNames.Purchase.SUPPLIER_ID, nullable = false)
  var supplierId: UUID,

  @Convert(converter = PurchaseStatusConverter::class)
  @Column(name = "purchase_status", nullable = false, length = 5)
  var purchaseStatus: PurchaseStatus = PurchaseStatus.DRAFT,

  @Convert(converter = PaymentStatusConverter::class)
  @Column(name = "payment_status", nullable = false, length = 5)
  var paymentStatus: PaymentStatus = PaymentStatus.UNPAID,

  @Column(name = "notes")
  var notes: String? = null,

  @Column(name = "date_ordered")
  var dateOrdered: OffsetDateTime? = null,

  @Column(name = "ordered_by")
  var orderedById: UUID? = null

) : HasReferenceEntity()
