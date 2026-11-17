package app.myliberty.easy_records.organizations.business.core.org_expense

import app.myliberty.easy_records.cross_tier.core.expense.repository.ExpenseVoidRepositoryBase
import org.springframework.stereotype.Repository

@Repository
interface OrgExpenseVoidRepository : ExpenseVoidRepositoryBase<OrgExpenseVoidEntity>
