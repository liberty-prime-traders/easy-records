package app.myliberty.easy_records.locations.business.expense

import app.myliberty.easy_records.cross_tier.expense.repository.ExpenseVoidRepositoryBase
import org.springframework.stereotype.Repository

@Repository
interface ExpenseVoidRepository : ExpenseVoidRepositoryBase<ExpenseVoidEntity>
