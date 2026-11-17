package app.myliberty.easy_records.organizations.rest.endpoints

import app.myliberty.easy_records.organizations.business.fiscal_period.api.FiscalPeriodRenameDto
import app.myliberty.easy_records.organizations.business.fiscal_period.api.FiscalPeriodResponseDto
import app.myliberty.easy_records.organizations.business.fiscal_period.api.FiscalPeriodService
import app.myliberty.easy_records.organizations.business.fiscal_period.api.YearEndCloseService
import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.util.annotations.RequiresFeature
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/fiscal-periods")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class FiscalPeriodEndpoint(
    private val fiscalPeriodService: FiscalPeriodService,
    private val yearEndCloseService: YearEndCloseService
) {

    @GetMapping
    fun getAll(): List<FiscalPeriodResponseDto> = fiscalPeriodService.getAll()

    @PutMapping("rename")
    fun rename(@RequestBody fiscalPeriodRenameDto: FiscalPeriodRenameDto): FiscalPeriodResponseDto =
        fiscalPeriodService.rename(fiscalPeriodRenameDto)

    @PostMapping("close")
    fun close(@RequestBody ids: Set<UUID>): List<FiscalPeriodResponseDto> = fiscalPeriodService.close(ids)

    @PostMapping("{id}/year-end-close")
    fun yearEndClose(@PathVariable id: UUID): FiscalPeriodResponseDto = yearEndCloseService.close(id)

    @PostMapping("nudge")
    fun nudgePeriodGeneration(): ResponseEntity<Void> {
        fiscalPeriodService.nudgePeriodGeneration()
        return ResponseEntity.ok().build()
    }
}
