package app.myliberty.easy_records.organizations.business.loan.search

import java.time.OffsetDateTime
import java.util.UUID

data class LoanSearchRow(
    val id: UUID,
    val createdOn: OffsetDateTime
)
