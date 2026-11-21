package app.myliberty.easy_records.organizations.business.core.feature.api

import app.myliberty.easy_records.organizations.business.core.feature.OrganizationFeatureStatus
import app.myliberty.easy_records.platform.business.feature.api.Feature
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationFeatureResponseDto(
    val id: UUID?,
    val feature: Feature,
    val status: OrganizationFeatureStatus,
    val enabledOn: OffsetDateTime?,
    val enabledBy: String?,
    val disabledOn: OffsetDateTime?,
    val disabledBy: String?
) : Serializable
