package app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.OrgJurisdictionTaxTypeCache
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class OrgJurisdictionTaxTypeFetcher(private val orgJurisdictionTaxTypeCache: OrgJurisdictionTaxTypeCache) {

    fun getAllDtos(): Collection<OrgJurisdictionTaxTypeDto> = orgJurisdictionTaxTypeCache.getAll()

    fun getAlreadyAssignedIds(): Set<UUID> =
        orgJurisdictionTaxTypeCache.getAll().mapTo(HashSet()) { it.jurisdictionTaxTypeId }
}
