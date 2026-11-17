package app.myliberty.easy_records.organizations.business.loan

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.ImmutableEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = TableNames.LOAN_VOID)
@HasReference(tableName = TableName.LOAN_VOID)
class LoanVoidEntity(

    @Column(name = "loan_id", nullable = false, updatable = false, unique = true)
    var loanId: UUID,

    @Column(name = "void_date", nullable = false, updatable = false)
    var voidDate: LocalDate,

    @Column(name = "reason", updatable = false)
    var reason: String?

) : ImmutableEntity()
