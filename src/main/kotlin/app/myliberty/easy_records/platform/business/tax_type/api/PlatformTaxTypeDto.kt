package app.myliberty.easy_records.platform.business.tax_type.api

import java.io.Serializable
import java.util.UUID

data class PlatformTaxTypeDto(
    val id: UUID,
    val label: String,
    val taxRecoveryType: TaxRecoveryType
) : Serializable
