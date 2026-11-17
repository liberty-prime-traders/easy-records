package app.myliberty.easy_records.platform.business.feature.api

import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class FeatureConverter : EnumConverter<Feature>(Feature::class.java)
