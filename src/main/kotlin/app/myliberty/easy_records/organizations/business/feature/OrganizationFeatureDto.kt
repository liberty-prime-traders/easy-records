package app.myliberty.easy_records.organizations.business.feature

import app.myliberty.easy_records.platform.business.feature.api.Feature
import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationFeatureDto(
    val id: UUID? = null,
    val createdById: UUID? = null,
    val createdOn: OffsetDateTime? = null,
    val feature: Feature,
    val status: OrganizationFeatureStatus,
    val enabledOn: OffsetDateTime? = null,
    val enabledBy: UUID? = null,
    val disabledOn: OffsetDateTime? = null,
    val disabledBy: UUID? = null
)
