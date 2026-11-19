package app.myliberty.easy_records.organizations.business.core.opening_balance

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.ImmutableEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.math.BigDecimal

@Entity
@Table(name = TableNames.OPENING_BALANCE)
@HasReference(tableName = TableName.OPENING_BALANCE)
class OpeningBalanceEntity(

    @Column(name = "account_code", nullable = false, updatable = false)
    var accountCode: String,

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4)
    var amount: BigDecimal

) : ImmutableEntity()
