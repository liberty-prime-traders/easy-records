package app.myliberty.easy_records.locations.business.retail.sale_payment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.ColumnNames
import app.myliberty.easy_records.util.model.ImmutableEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.SALE_PAYMENT_VOID)
@HasReference(tableName = TableName.SALE_PAYMENT_VOID)
class SalePaymentVoidEntity(

    @Column(name = ColumnNames.SalePaymentVoid.SALE_PAYMENT_ID, nullable = false, updatable = false, unique = true)
    var salePaymentId: UUID,

    @Column(name = ColumnNames.PaymentVoid.REASON, nullable = false, updatable = false)
    var reason: String

) : ImmutableEntity()
