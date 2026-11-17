package app.myliberty.easy_records.platform.business.jurisdiction_tax_type.api

import java.io.Serializable
import java.util.UUID

data class JurisdictionTaxTypeInsertDto(
    val taxTypeId: UUID,
    val jurisdictionId: UUID
) : Serializable
