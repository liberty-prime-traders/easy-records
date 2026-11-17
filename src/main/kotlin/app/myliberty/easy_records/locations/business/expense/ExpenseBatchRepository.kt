package app.myliberty.easy_records.locations.business.expense

import app.myliberty.easy_records.cross_tier.expense.repository.ExpenseBatchRepositoryBase
import org.springframework.stereotype.Repository

@Repository
interface ExpenseBatchRepository : ExpenseBatchRepositoryBase<ExpenseBatchEntity>
