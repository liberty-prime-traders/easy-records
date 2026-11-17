package app.myliberty.easy_records.util.enums

import jakarta.persistence.Converter

@Converter(autoApply = true)
class PaymentStatusConverter : EnumConverter<PaymentStatus>(PaymentStatus::class.java)
