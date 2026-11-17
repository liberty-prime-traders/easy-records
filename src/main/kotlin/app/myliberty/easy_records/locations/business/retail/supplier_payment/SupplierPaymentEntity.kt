package app.myliberty.easy_records.locations.business.retail.supplier_payment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.ColumnNames
import app.myliberty.easy_records.util.model.ImmutableEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = TableNames.SUPPLIER_PAYMENT)
@HasReference(tableName = TableName.SUPPLIER_PAYMENT)
class SupplierPaymentEntity(

    @Column(name = ColumnNames.SupplierPayment.PURCHASE_ID, nullable = false, updatable = false)
    var purchaseId: UUID,

    @Column(name = "delivery_id", updatable = false)
    var deliveryId: UUID? = null,

    @Column(name = ColumnNames.Payment.PAYMENT_METHOD_ID, nullable = false, updatable = false)
    var paymentMethodId: UUID,

    @Column(name = "payment_method_account_code", nullable = false, updatable = false, length = 30)
    var paymentMethodAccountCode: String,

    @Column(name = ColumnNames.Payment.AMOUNT, nullable = false, precision = 19, scale = 4, updatable = false)
    var amount: BigDecimal,

    @Column(name = ColumnNames.Payment.PAYMENT_DATE, nullable = false, updatable = false)
    var paymentDate: OffsetDateTime,

    @Column(name = "notes")
    var notes: String? = null

) : ImmutableEntity()
