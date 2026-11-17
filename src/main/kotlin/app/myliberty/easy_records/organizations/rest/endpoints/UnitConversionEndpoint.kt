package app.myliberty.easy_records.organizations.rest.endpoints

import app.myliberty.easy_records.organizations.business.unitconversion.api.ConversionTargetDto
import app.myliberty.easy_records.organizations.business.unitconversion.api.UnitConversionGraphFacade
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("secured/unit-conversions")
class UnitConversionEndpoint(private val unitConversionGraphFacade: UnitConversionGraphFacade) {

    @GetMapping("graph")
    fun getGraph(): Map<UUID, Map<UUID, ConversionTargetDto>> = unitConversionGraphFacade.getOrLoad().getFullGraph()
}
