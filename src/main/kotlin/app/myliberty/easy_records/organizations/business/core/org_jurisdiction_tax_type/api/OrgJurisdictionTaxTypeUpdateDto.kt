package app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api

import app.myliberty.easy_records.util.business.StringUtils
import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class OrgJurisdictionTaxTypeUpdateDto(
    val id: UUID,
    val status: OrgJurisdictionTaxTypeStatus? = null,
    val payableAccountCode: String? = null,
    val recoverableAccountCode: Optional<String>? = null
) : Serializable {

    fun applyTo(existing: OrgJurisdictionTaxTypeDto): OrgJurisdictionTaxTypeDto = existing.copy(
        status = status ?: existing.status,
        payableAccountCode = StringUtils.getValueOrNull(payableAccountCode) ?: existing.payableAccountCode,
        recoverableAccountCode = StringUtils.useIfProvided(recoverableAccountCode, existing.recoverableAccountCode)
    )
}
