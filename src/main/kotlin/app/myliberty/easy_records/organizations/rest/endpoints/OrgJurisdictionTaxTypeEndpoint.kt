package app.myliberty.easy_records.organizations.rest.endpoints

import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeInsertDto
import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeResponseDto
import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeService
import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeUpdateDto
import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.platform.business.jurisdiction_tax_type.api.JurisdictionTaxesTreeBuilder
import app.myliberty.easy_records.platform.business.tax_type.api.TaxRecoveryType
import app.myliberty.easy_records.util.annotations.RequiresFeature
import app.myliberty.easy_records.util.ui_models.TreeNodeWithData
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/org-tax-types")
@PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
@RequiresFeature(Feature.TAX_CONFIGURATION)
class OrgJurisdictionTaxTypeEndpoint(
    private val orgJurisdictionTaxTypeService: OrgJurisdictionTaxTypeService,
    private val jurisdictionTaxesTreeBuilder: JurisdictionTaxesTreeBuilder
) {

    @GetMapping
    fun getAll(): List<OrgJurisdictionTaxTypeResponseDto> = orgJurisdictionTaxTypeService.getAll()

    @GetMapping("available")
    fun getTree(): List<TreeNodeWithData<UUID, TaxRecoveryType>> = jurisdictionTaxesTreeBuilder.getAvailableTaxTypes()

    @PostMapping
    fun createAll(@RequestBody dtos: List<OrgJurisdictionTaxTypeInsertDto>): List<OrgJurisdictionTaxTypeResponseDto> =
        orgJurisdictionTaxTypeService.createAll(dtos)

    @PutMapping
    fun update(@RequestBody dto: OrgJurisdictionTaxTypeUpdateDto): OrgJurisdictionTaxTypeResponseDto =
        orgJurisdictionTaxTypeService.update(dto)
}
