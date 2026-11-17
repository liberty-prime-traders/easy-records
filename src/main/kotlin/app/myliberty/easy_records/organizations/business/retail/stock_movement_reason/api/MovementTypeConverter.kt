package app.myliberty.easy_records.organizations.business.stock_movement_reason.api

import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class MovementTypeConverter : EnumConverter<MovementType>(MovementType::class.java)
