package app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api

import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class AdjustmentDirectionConverter :
    EnumConverter<AdjustmentDirection>(AdjustmentDirection::class.java)
