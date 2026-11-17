package app.myliberty.easy_records.organizations.rest.endpoints

import app.myliberty.easy_records.organizations.business.core.feature.api.OrganizationFeatureResponseDto
import app.myliberty.easy_records.organizations.business.core.feature.api.OrganizationFeatureService
import app.myliberty.easy_records.platform.business.feature.api.Feature
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/org-features")
class OrganizationFeatureEndpoint(private val orgFeatureService: OrganizationFeatureService) {

    @GetMapping
    fun getAll(): List<OrganizationFeatureResponseDto> = orgFeatureService.getAll()

    @PostMapping("activate")
    fun activate(@RequestBody features: HashSet<Feature>): List<OrganizationFeatureResponseDto> =
        orgFeatureService.activate(features)

    @PostMapping("deactivate")
    fun deactivate(@RequestBody features: HashSet<Feature>): List<OrganizationFeatureResponseDto> =
        orgFeatureService.deactivate(features)
}
