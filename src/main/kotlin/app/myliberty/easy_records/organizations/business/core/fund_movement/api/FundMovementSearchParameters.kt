package app.myliberty.easy_records.organizations.business.core.fund_movement.api

import app.myliberty.easy_records.util.business.StringUtils
import java.time.LocalDate
import java.time.OffsetDateTime

data class FundMovementSearchParameters(
    val createdFrom: OffsetDateTime? = null,
    val createdBefore: OffsetDateTime? = null,
    val types: Set<FundMovementType> = emptySet(),
    val accountCodes: List<String> = emptyList(),
    val movementDateFrom: LocalDate? = null,
    val movementDateBefore: LocalDate? = null,
    val statuses: Set<FundMovementStatus> = emptySet(),
    val references: List<String> = emptyList()
) {

    fun sanitized(): FundMovementSearchParameters = copy(
        accountCodes = StringUtils.dropBlank(accountCodes),
        references = StringUtils.dropBlank(references)
    )
}
