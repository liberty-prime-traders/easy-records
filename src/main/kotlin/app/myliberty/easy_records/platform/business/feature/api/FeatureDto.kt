package app.myliberty.easy_records.platform.business.feature.api

import java.util.UUID

data class FeatureDto(
    val id: UUID,
    val feature: Feature,
    val description: String?
)
