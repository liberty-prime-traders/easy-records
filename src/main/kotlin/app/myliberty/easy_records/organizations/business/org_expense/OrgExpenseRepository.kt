package app.myliberty.easy_records.organizations.business.org_expense

import app.myliberty.easy_records.cross_tier.expense.repository.ExpenseRepositoryBase
import org.springframework.stereotype.Repository

@Repository
interface OrgExpenseRepository : ExpenseRepositoryBase<OrgExpenseEntity>
