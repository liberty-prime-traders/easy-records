package app.myliberty.easy_records.platform.business.organization.api

import app.myliberty.easy_records.organizations.business.core.location.api.LocationResponseDto

data class OrganizationWithLocations(
    val organization: OrganizationDto,
    val locations: Collection<LocationResponseDto>
)
