package app.myliberty.easy_records.cross_tier.core.expense.operation

import java.util.UUID

interface ExpenseReissuer {

    val isLocationLevel: Boolean

    fun reissueRecorded(expenseId: UUID)

    fun reissueVoided(expenseVoidId: UUID)

    fun reissuePaymentRecorded(paymentId: UUID)

    fun reissuePaymentVoided(paymentVoidId: UUID)
}
