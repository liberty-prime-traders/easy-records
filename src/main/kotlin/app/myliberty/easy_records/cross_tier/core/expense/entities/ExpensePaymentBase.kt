package app.myliberty.easy_records.cross_tier.core.expense.entities

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import app.myliberty.easy_records.util.model.ImmutableEntity
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@MappedSuperclass
abstract class ExpensePaymentBase(

    @Column(name = "expense_id", nullable = false, updatable = false)
    var expenseId: UUID,

    @Column(name = "payment_method_id", nullable = false, updatable = false)
    var paymentMethodId: UUID,

    @Column(name = "payment_method_account_code", nullable = false, updatable = false, length = 30)
    var paymentMethodAccountCode: String,

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4)
    var amount: BigDecimal,

    @Column(name = "provider_reference", updatable = false)
    var providerReference: String?,

    @Column(name = "payment_date", nullable = false, updatable = false)
    var paymentDate: OffsetDateTime

) : ImmutableEntity()
