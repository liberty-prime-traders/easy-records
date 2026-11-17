package app.myliberty.easy_records.organizations.rest.endpoints

import app.myliberty.easy_records.organizations.business.core.accounting_config.api.OrgAccountingConfigResponseDto
import app.myliberty.easy_records.organizations.business.core.accounting_config.api.OrgAccountingConfigService
import app.myliberty.easy_records.organizations.business.core.accounting_config.api.OrgAccountingConfigUpdateDto
import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.util.annotations.RequiresFeature
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/accounting-config")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class OrgAccountingConfigEndpoint(
    private val service: OrgAccountingConfigService
) {

    @GetMapping
    fun get(): OrgAccountingConfigResponseDto = service.get()

    @PutMapping
    fun update(@RequestBody dto: OrgAccountingConfigUpdateDto): OrgAccountingConfigResponseDto = service.update(dto)
}
