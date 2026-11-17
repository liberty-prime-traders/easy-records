package app.myliberty.easy_records.locations.business.retail.sale_payment

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
@Table(name = TableNames.SALE_PAYMENT)
@HasReference(tableName = TableName.SALE_PAYMENT)
class SalePaymentEntity(

    @Column(name = ColumnNames.SalePayment.SALE_ID, nullable = false, updatable = false)
    var saleId: UUID,

    @Column(name = ColumnNames.Payment.PAYMENT_METHOD_ID, nullable = false, updatable = false)
    var paymentMethodId: UUID,

    @Column(name = "payment_method_account_code", nullable = false, updatable = false, length = 30)
    var paymentMethodAccountCode: String,

    @Column(name = ColumnNames.Payment.AMOUNT, nullable = false, precision = 19, scale = 4, updatable = false)
    var amount: BigDecimal,

    @Column(name = "reference", updatable = false)
    var reference: String? = null,

    @Column(name = ColumnNames.Payment.PAYMENT_DATE, updatable = false)
    var paymentDate: OffsetDateTime

) : ImmutableEntity()
