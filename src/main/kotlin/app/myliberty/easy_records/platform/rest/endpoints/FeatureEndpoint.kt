package app.myliberty.easy_records.platform.rest.endpoints

import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.platform.business.feature.api.FeatureDto
import app.myliberty.easy_records.platform.business.feature.api.FeatureService
import app.myliberty.easy_records.platform.business.feature.api.FeatureUpdateDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/platform-features")
class FeatureEndpoint(private val featureService: FeatureService) {

    @GetMapping
    fun getAll(): Collection<FeatureDto> = featureService.getAll()

    @PutMapping
    fun updateDescription( @RequestBody updateDto: FeatureUpdateDto) =
        featureService.updateDescription(updateDto)

    @PostMapping("{organizationId}/activate")
    fun activate(@PathVariable organizationId: UUID, @RequestBody features: HashSet<Feature>) =
        featureService.activate(organizationId, features)

    @PostMapping("{organizationId}/deactivate")
    fun deactivate(@PathVariable organizationId: UUID, @RequestBody features: HashSet<Feature>) =
        featureService.deactivate(organizationId, features)
}
