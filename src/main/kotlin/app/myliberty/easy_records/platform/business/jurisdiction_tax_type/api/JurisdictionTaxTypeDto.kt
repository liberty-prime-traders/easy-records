package app.myliberty.easy_records.platform.business.jurisdiction_tax_type.api

import java.time.OffsetDateTime
import java.util.UUID

data class JurisdictionTaxTypeDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val taxTypeId: UUID,
    val jurisdictionId: UUID,
    val active: Boolean = true
)
