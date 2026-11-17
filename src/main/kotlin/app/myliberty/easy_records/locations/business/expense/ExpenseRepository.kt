package app.myliberty.easy_records.locations.business.expense

import app.myliberty.easy_records.cross_tier.expense.repository.ExpenseRepositoryBase
import org.springframework.stereotype.Repository

@Repository
interface ExpenseRepository : ExpenseRepositoryBase<ExpenseEntity>
