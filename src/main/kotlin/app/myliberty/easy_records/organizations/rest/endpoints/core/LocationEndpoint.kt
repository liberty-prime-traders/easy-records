package app.myliberty.easy_records.organizations.rest.endpoints.core

import app.myliberty.easy_records.organizations.business.core.location.api.LocationInsertDto
import app.myliberty.easy_records.organizations.business.core.location.api.LocationResponseDto
import app.myliberty.easy_records.organizations.business.core.location.api.LocationService
import app.myliberty.easy_records.organizations.business.core.location.api.LocationUpdateDto
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin
@RestController
@RequestMapping("secured/locations")
class LocationEndpoint(private val locationService: LocationService) {

    @GetMapping
    @PreAuthorize("@esrPermissions.isOrganizationMember()")
    fun getForOrganization(): Collection<LocationResponseDto> =
        locationService.getAllLocations()

    @PostMapping
    @PreAuthorize("@esrPermissions.isOrganizationAdmin()")
    fun createLocation(@RequestBody locationInsertDto: LocationInsertDto): LocationResponseDto =
        locationService.createLocation(locationInsertDto)

    @PutMapping
    @PreAuthorize("@esrPermissions.isOrganizationAdmin()")
    fun updateLocation(@RequestBody locationUpdateDto: LocationUpdateDto): LocationResponseDto =
        locationService.updateLocation(locationUpdateDto)

}
