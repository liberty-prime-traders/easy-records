package app.myliberty.easy_records.organizations.business.loan

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.ImmutableEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = TableNames.LOAN_REPAYMENT)
@HasReference(tableName = TableName.LOAN_REPAYMENT)
class LoanRepaymentEntity(

    @Column(name = "loan_id", nullable = false, updatable = false)
    var loanId: UUID,

    @Column(name = "asset_account_code", nullable = false, updatable = false)
    var assetAccountCode: String,

    @Column(name = "amount", nullable = false, updatable = false)
    var amount: BigDecimal,

    @Column(name = "repayment_date", nullable = false, updatable = false)
    var repaymentDate: LocalDate,

    @Column(name = "note", updatable = false)
    var note: String?

) : ImmutableEntity()
