package app.myliberty.easy_records.locations.business.core.expense

import app.myliberty.easy_records.cross_tier.core.expense.repository.ExpenseBatchRepositoryBase
import org.springframework.stereotype.Repository

@Repository
interface ExpenseBatchRepository : ExpenseBatchRepositoryBase<ExpenseBatchEntity>
