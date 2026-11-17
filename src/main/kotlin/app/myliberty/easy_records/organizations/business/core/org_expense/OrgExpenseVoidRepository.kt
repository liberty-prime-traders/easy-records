package app.myliberty.easy_records.organizations.business.org_expense

import app.myliberty.easy_records.cross_tier.expense.repository.ExpenseVoidRepositoryBase
import org.springframework.stereotype.Repository

@Repository
interface OrgExpenseVoidRepository : ExpenseVoidRepositoryBase<OrgExpenseVoidEntity>
