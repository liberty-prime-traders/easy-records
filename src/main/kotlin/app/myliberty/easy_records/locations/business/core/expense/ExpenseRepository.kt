package app.myliberty.easy_records.locations.business.core.expense

import app.myliberty.easy_records.cross_tier.core.expense.repository.ExpenseRepositoryBase
import org.springframework.stereotype.Repository

@Repository
interface ExpenseRepository : ExpenseRepositoryBase<ExpenseEntity>
