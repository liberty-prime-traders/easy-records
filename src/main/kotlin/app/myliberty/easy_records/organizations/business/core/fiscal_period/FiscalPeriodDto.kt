package app.myliberty.easy_records.organizations.business.fiscal_period

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class FiscalPeriodDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val yearEnd: Boolean,
    val stub: Boolean,
val closedAt: Instant?,
    val closedBy: UUID?
) {
    val isClosed: Boolean = closedAt != null
}
