package app.myliberty.easy_records.locations.business.sale

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.ImmutableEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.SALE_VOID)
@HasReference(tableName = TableName.SALE_VOID)
class SaleVoidEntity(

    @Column(name = "sale_id", nullable = false, updatable = false, unique = true)
    var saleId: UUID,

    @Column(name = "reason", nullable = false, updatable = false)
    var reason: String

) : ImmutableEntity()
