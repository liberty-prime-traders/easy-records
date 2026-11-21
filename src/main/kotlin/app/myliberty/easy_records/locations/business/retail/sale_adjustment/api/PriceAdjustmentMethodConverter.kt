package app.myliberty.easy_records.locations.business.retail.sale_adjustment.api

import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class PriceAdjustmentMethodConverter : EnumConverter<PriceAdjustmentMethod>(PriceAdjustmentMethod::class.java)
