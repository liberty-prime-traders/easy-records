package app.myliberty.easy_records.cross_tier.core.expense.entities

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import app.myliberty.easy_records.util.model.ImmutableEntity
import java.util.UUID

@MappedSuperclass
abstract class ExpenseVoidBase(

    @Column(name = "expense_id", nullable = false, updatable = false, unique = true)
    var expenseId: UUID,

    @Column(name = "reason", nullable = false, updatable = false)
    var reason: String

) : ImmutableEntity()
