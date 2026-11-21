package app.myliberty.easy_records.platform.business.tax_type.api

import java.io.Serializable

data class TaxTypeInsertDto(
    val name: String,
    val description: String? = null,
    val calculationMethod: TaxCalculationMethod,
    val taxRecoveryType: TaxRecoveryType,
    val taxApplicationLevel: TaxApplicationLevel,
    val taxTriggers: Set<TaxTrigger>
) : Serializable
