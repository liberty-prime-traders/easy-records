package app.myliberty.easy_records.organizations.business.core.org_expense

import app.myliberty.easy_records.cross_tier.core.expense.repository.ExpenseBatchRepositoryBase
import org.springframework.stereotype.Repository

@Repository
interface OrgExpenseBatchRepository : ExpenseBatchRepositoryBase<OrgExpenseBatchEntity>
