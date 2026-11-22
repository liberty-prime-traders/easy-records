package app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api

import app.myliberty.easy_records.platform.business.tax_type.api.TaxRecoveryType
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class OrgJurisdictionTaxTypeResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val createdOn: OffsetDateTime?,
    val jurisdictionTaxTypeId: UUID,
    val platformTaxId: UUID,
    val taxLabel: String,
    val taxRecoveryType: TaxRecoveryType,
    val status: OrgJurisdictionTaxTypeStatus,
    val payableAccountCode: String?,
    val payableAccount: String?,
    val recoverableAccountCode: String?,
    val recoverableAccount: String?
) : Serializable
