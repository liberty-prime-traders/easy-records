package app.myliberty.easy_records.platform.business.organization_join_request.api

import app.myliberty.easy_records.platform.business.organization.api.OrganizationResponseDto
import java.io.Serializable

data class OrganizationLaunchResponseDto(
    val organization: OrganizationResponseDto? = null,
    val isOrganizationAdmin: Boolean,
    val accessRequested: Boolean
) : Serializable
