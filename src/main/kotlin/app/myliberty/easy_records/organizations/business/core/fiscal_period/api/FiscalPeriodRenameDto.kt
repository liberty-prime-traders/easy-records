package app.myliberty.easy_records.organizations.business.core.fiscal_period.api

import java.util.UUID

data class FiscalPeriodRenameDto(
    val id: UUID,
    val name: String
)
