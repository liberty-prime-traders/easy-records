package app.myliberty.easy_records.cross_tier.core.expense.kafka_handler

import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.cross_tier.core.expense.operation.ExpenseReissuer
import org.springframework.stereotype.Component

@Component
class ExpenseReissuerResolver(
    private val expenseReissuers: List<ExpenseReissuer>
) {

    fun current(): ExpenseReissuer {
        val isLocationLevel = SessionContextProvider.getLocationIdOrNull() != null
        return expenseReissuers.single { it.isLocationLevel == isLocationLevel }
    }
}
