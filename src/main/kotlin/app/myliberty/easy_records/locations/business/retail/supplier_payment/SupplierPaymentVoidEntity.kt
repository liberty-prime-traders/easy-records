package app.myliberty.easy_records.locations.business.supplier_payment

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
@Table(name = TableNames.SUPPLIER_PAYMENT_VOID)
@HasReference(tableName = TableName.SUPPLIER_PAYMENT_VOID)
class SupplierPaymentVoidEntity(

    @Column(name = ColumnNames.SupplierPaymentVoid.SUPPLIER_PAYMENT_ID, nullable = false, updatable = false, unique = true)
    var supplierPaymentId: UUID,

    @Column(name = ColumnNames.PaymentVoid.REASON, nullable = false, updatable = false)
    var reason: String

) : ImmutableEntity()
