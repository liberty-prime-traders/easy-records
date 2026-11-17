package app.myliberty.easy_records.organizations.business.core.loan

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
@Table(name = TableNames.LOAN)
@HasReference(tableName = TableName.LOAN)
class LoanEntity(

    @Column(name = "lender_contact_id", nullable = false, updatable = false)
    var lenderContactId: UUID,

    @Column(name = "asset_account_code", nullable = false, updatable = false)
    var assetAccountCode: String,

    @Column(name = "amount", nullable = false, updatable = false)
    var amount: BigDecimal,

    @Column(name = "loan_date", nullable = false, updatable = false)
    var loanDate: LocalDate,

    @Column(name = "note", updatable = false)
    var note: String?

) : ImmutableEntity()
