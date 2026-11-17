package app.myliberty.easy_records.organizations.rest.endpoints.core

import app.myliberty.easy_records.organizations.business.core.tax_rate.api.TaxRateInsertDto
import app.myliberty.easy_records.organizations.business.core.tax_rate.api.TaxRateResponseDto
import app.myliberty.easy_records.organizations.business.core.tax_rate.api.TaxRateService
import app.myliberty.easy_records.organizations.business.core.tax_rate.api.TaxRateUpdateDto
import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.util.annotations.RequiresFeature
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/tax-rates")
@RequiresFeature(Feature.TAX_CONFIGURATION)
class TaxRateEndpoint(private val taxRateService: TaxRateService) {

    @GetMapping
    fun getAll(): Collection<TaxRateResponseDto> = taxRateService.getAll()

    @PostMapping
    fun create(@RequestBody dto: TaxRateInsertDto): TaxRateResponseDto =
        taxRateService.create(dto)

    @PutMapping
    fun update(@RequestBody dto: TaxRateUpdateDto): TaxRateResponseDto =
        taxRateService.update(dto)
}
