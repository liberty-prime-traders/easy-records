package app.myliberty.easy_records.platform.business.tax_type.api

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class TaxTypeResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val createdOn: OffsetDateTime?,
    val name: String,
    val description: String?,
    val calculationMethod: TaxCalculationMethod,
    val taxRecoveryType: TaxRecoveryType,
    val taxApplicationLevel: TaxApplicationLevel,
    val taxTriggers: Set<TaxTrigger>
) : Serializable
