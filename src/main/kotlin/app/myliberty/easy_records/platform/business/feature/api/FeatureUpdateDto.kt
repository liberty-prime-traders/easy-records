package app.myliberty.easy_records.platform.business.feature.api

import app.myliberty.easy_records.util.business.StringUtils
import java.util.Optional


data class FeatureUpdateDto(
    val feature: Feature,
    val description: Optional<String>? = null
) {
    fun applyTo(existing: FeatureDto): FeatureDto {
        return existing.copy(
            description = StringUtils.useIfProvided(description, existing.description)
        )
    }
}
