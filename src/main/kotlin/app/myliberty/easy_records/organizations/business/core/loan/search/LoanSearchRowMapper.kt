package app.myliberty.easy_records.organizations.business.core.loan.search

import jakarta.persistence.Tuple
import app.myliberty.easy_records.util.business.mappers.DateQualifier
import java.time.Instant
import java.util.UUID

object LoanSearchRowMapper {

    fun fromTuple(tuple: Tuple): LoanSearchRow = LoanSearchRow(
        id = tuple.get("id", UUID::class.java),
        createdOn = requireNotNull(DateQualifier.toOffsetDateTime(tuple.get("created_on", Instant::class.java)))
    )
}
