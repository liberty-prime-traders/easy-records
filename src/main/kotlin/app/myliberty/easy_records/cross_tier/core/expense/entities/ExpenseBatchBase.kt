package app.myliberty.easy_records.cross_tier.core.expense.entities

import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.MappedSuperclass
import app.myliberty.easy_records.util.model.ImmutableEntity

@MappedSuperclass
abstract class ExpenseBatchBase(

    @Column(name = "description", nullable = false, updatable = false)
    var description: String,

    @Convert(converter = ExpenseSourceTypeConverter::class)
    @Column(name = "source_type", nullable = false, updatable = false, length = 5)
    var sourceType: ExpenseSourceType,

    @Column(name = "source_reference", updatable = false)
    var sourceReference: String?

) : ImmutableEntity()
