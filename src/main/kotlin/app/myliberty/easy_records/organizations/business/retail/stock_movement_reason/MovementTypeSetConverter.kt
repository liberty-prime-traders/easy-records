package app.myliberty.easy_records.organizations.business.retail.stock_movement_reason

import jakarta.persistence.Converter
import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.MovementType
import app.myliberty.easy_records.util.enums.EnumSetConverter

@Converter(autoApply = true)
class MovementTypeSetConverter : EnumSetConverter<MovementType>(MovementType::class.java)
