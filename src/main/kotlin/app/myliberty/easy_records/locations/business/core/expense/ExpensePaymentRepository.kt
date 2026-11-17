package app.myliberty.easy_records.locations.business.expense

import app.myliberty.easy_records.cross_tier.expense.repository.ExpensePaymentRepositoryBase
import org.springframework.stereotype.Repository

@Repository
interface ExpensePaymentRepository : ExpensePaymentRepositoryBase<ExpensePaymentEntity>
