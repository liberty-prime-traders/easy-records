package app.myliberty.easy_records.organizations.business.core.expense_type

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactType
import app.myliberty.easy_records.util.model.HasCreatorEntity
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited

@Audited
@Entity
@Table(name = TableNames.EXPENSE_TYPE)
class ExpenseTypeEntity(

    @NotAudited
    @Column(name = "code", updatable = false)
    var code: String? = null,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "expense_account_code", nullable = false)
    var expenseAccountCode: String,

    @Column(name = "eligible_payee_types", nullable = false)
    @Convert(converter = ExpensePayeeTypeSetConverter::class)
    var eligiblePayeeTypes: Set<ContactType>,

    @Column(name = "eligible_source_types", nullable = false)
    @Convert(converter = ExpenseSourceTypeSetConverter::class)
    var eligibleSourceTypes: Set<ExpenseSourceType>,

    @NotAudited
    @Column(name = "system_defined", nullable = false, updatable = false)
    var systemDefined: Boolean = false

) : HasCreatorEntity()
