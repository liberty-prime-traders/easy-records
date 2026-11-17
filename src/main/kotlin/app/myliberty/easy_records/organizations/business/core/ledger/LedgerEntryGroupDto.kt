package app.myliberty.easy_records.organizations.business.ledger

import app.myliberty.easy_records.organizations.business.ledger.api.LedgerSourceType
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

data class LedgerEntryGroupDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime  ,
    val referenceNumber: String,
    val sourceReferenceNumber: String,
    val sourceType: LedgerSourceType,
    val sourceLocationId: UUID?,
    val fiscalPeriodId: UUID,
    val postedOn: Instant
)
