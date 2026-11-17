package app.myliberty.easy_records.cross_tier.core.expense.entities

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import app.myliberty.easy_records.util.enums.PaymentStatus
import app.myliberty.easy_records.util.enums.PaymentStatusConverter
import app.myliberty.easy_records.util.model.HasCreatorEntity
import java.math.BigDecimal
import java.util.UUID

@MappedSuperclass
abstract class ExpensePaymentStateBase(

    @Column(name = "expense_id", nullable = false, updatable = false)
    var expenseId: UUID,

    @Convert(converter = PaymentStatusConverter::class)
    @Column(name = "payment_status", nullable = false, length = 5)
    var paymentStatus: PaymentStatus = PaymentStatus.UNPAID,

    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 4)
    var amountPaid: BigDecimal = BigDecimal.ZERO,

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0

) : HasCreatorEntity()
