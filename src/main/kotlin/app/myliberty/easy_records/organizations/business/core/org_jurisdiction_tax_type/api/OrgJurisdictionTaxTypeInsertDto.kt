package app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api

import java.util.UUID

data class OrgJurisdictionTaxTypeInsertDto(
    val jurisdictionTaxTypeId: UUID,
    val status: OrgJurisdictionTaxTypeStatus = OrgJurisdictionTaxTypeStatus.ACTIVE,
    val payableAccountCode: String,
    val recoverableAccountCode: String? = null
)
