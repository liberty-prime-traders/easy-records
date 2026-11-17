package app.myliberty.easy_records.cross_tier.expense.entities

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import app.myliberty.easy_records.util.model.ImmutableEntity
import java.util.UUID

@MappedSuperclass
abstract class ExpensePaymentVoidBase(

    @Column(name = "expense_payment_id", nullable = false, updatable = false, unique = true)
    var expensePaymentId: UUID,

    @Column(name = "reason", nullable = false, updatable = false)
    var reason: String

) : ImmutableEntity()
