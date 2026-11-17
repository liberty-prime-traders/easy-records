package app.myliberty.easy_records.organizations.business.fund_movement

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.organizations.business.fund_movement.api.FundMovementType
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.ImmutableEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = TableNames.FUND_MOVEMENT)
@HasReference(tableName = TableName.FUND_MOVEMENT)
class FundMovementEntity(

    @Convert(converter = FundMovementTypeConverter::class)
    @Column(name = "type", nullable = false, updatable = false, length = 5)
    var type: FundMovementType,

    @Column(name = "debit_account_code", nullable = false, updatable = false)
    var debitAccountCode: String,

    @Column(name = "credit_account_code", nullable = false, updatable = false)
    var creditAccountCode: String,

    @Column(name = "amount", nullable = false, updatable = false)
    var amount: BigDecimal,

    @Column(name = "movement_date", nullable = false, updatable = false)
    var movementDate: LocalDate,

    @Column(name = "note", updatable = false)
    var note: String?

) : ImmutableEntity()
