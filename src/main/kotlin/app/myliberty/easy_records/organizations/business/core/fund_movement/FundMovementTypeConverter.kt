package app.myliberty.easy_records.organizations.business.core.fund_movement

import jakarta.persistence.Converter
import app.myliberty.easy_records.organizations.business.core.fund_movement.api.FundMovementType
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter
class FundMovementTypeConverter : EnumConverter<FundMovementType>(FundMovementType::class.java)
