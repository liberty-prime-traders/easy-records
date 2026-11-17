package app.myliberty.easy_records.organizations.business.core.tax_rate.api

import app.myliberty.easy_records.platform.business.tax_type.api.TaxCalculationMethod
import app.myliberty.easy_records.util.business.Decimals
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import java.math.BigDecimal
import java.util.UUID

object TaxAmountCalculator {

    data class ActiveTaxTypeForCalculation(
        val orgJurisdictionTaxTypeId: UUID,
        val jurisdictionTaxTypeId: UUID,
        val calculationMethod: TaxCalculationMethod,
        val activeTaxRateDto: ActiveTaxRateDto
    )

    data class TaxCalculationEntry(
        val orgJurisdictionTaxTypeId: UUID,
        val jurisdictionTaxTypeId: UUID,
        val calculationMethod: TaxCalculationMethod,
        val rate: BigDecimal,
        val taxIsBilledToCustomerSeparately: Boolean,
        val taxIsIncludedInTaxableAmount: Boolean,
        val taxableAmount: BigDecimal,
        val resolvedTaxableBase: BigDecimal,
        val taxAmount: BigDecimal
    )

    data class TaxCalculationResult(
        val entries: List<TaxCalculationEntry>,
        val taxBilled: BigDecimal
    )

    fun calculate(
        taxableAmount: BigDecimal,
        activeTaxTypes: List<ActiveTaxTypeForCalculation>
    ): TaxCalculationResult {
        val includedFlatAmounts = activeTaxTypes
            .filter { it.activeTaxRateDto.taxIsIncludedInTaxableAmount && it.calculationMethod == TaxCalculationMethod.FIXED_VALUE }
            .fold(BigDecimal.ZERO) { acc, taxType -> acc + flatAmountOf(taxType) }
        val resolvedTaxableBase = resolveTaxableBase(taxableAmount, includedFlatAmounts, activeTaxTypes)
        val includedPercentageTaxAmounts = allocateIncludedPercentageTax(
            taxableAmount - includedFlatAmounts - resolvedTaxableBase, activeTaxTypes
        )

        val entries = activeTaxTypes.map { activeTaxType ->
            TaxCalculationEntry(
                orgJurisdictionTaxTypeId = activeTaxType.orgJurisdictionTaxTypeId,
                jurisdictionTaxTypeId = activeTaxType.jurisdictionTaxTypeId,
                calculationMethod = activeTaxType.calculationMethod,
                rate = activeTaxType.activeTaxRateDto.ratePercentage ?: activeTaxType.activeTaxRateDto.rateFlatAmount ?: BigDecimal.ZERO,
                taxIsBilledToCustomerSeparately = activeTaxType.activeTaxRateDto.taxIsBilledToCustomerSeparately,
                taxIsIncludedInTaxableAmount = activeTaxType.activeTaxRateDto.taxIsIncludedInTaxableAmount,
                taxableAmount = taxableAmount,
                resolvedTaxableBase = resolvedTaxableBase,
                taxAmount = taxAmountFor(activeTaxType, resolvedTaxableBase, includedPercentageTaxAmounts)
            )
        }

        return TaxCalculationResult(
            entries = entries,
            taxBilled = entries.filter { it.taxIsBilledToCustomerSeparately }.fold(BigDecimal.ZERO) { acc, entry -> acc + entry.taxAmount }
        )
    }

    private fun resolveTaxableBase(
        taxableAmount: BigDecimal,
        includedFlatAmounts: BigDecimal,
        activeTaxTypes: List<ActiveTaxTypeForCalculation>
    ): BigDecimal {
        val includedPercentageSum = activeTaxTypes
            .filter { it.activeTaxRateDto.taxIsIncludedInTaxableAmount }
            .filter { it.calculationMethod == TaxCalculationMethod.PERCENTAGE }
            .fold(BigDecimal.ZERO) { acc, taxType -> acc + requirePercentage(taxType) }
        return Decimals.MoneyScale.divide((taxableAmount - includedFlatAmounts) * BigDecimal(100), BigDecimal(100) + includedPercentageSum)
    }

    private fun allocateIncludedPercentageTax(
        includedPercentageTotal: BigDecimal,
        activeTaxTypes: List<ActiveTaxTypeForCalculation>
    ): Map<UUID, BigDecimal> {
        val includedPercentageTaxTypes = activeTaxTypes.filter {
            it.activeTaxRateDto.taxIsIncludedInTaxableAmount && it.calculationMethod == TaxCalculationMethod.PERCENTAGE
        }
        if (includedPercentageTaxTypes.isEmpty()) return emptyMap()
        val ratePercentageSum = includedPercentageTaxTypes.fold(BigDecimal.ZERO) { acc, taxType -> acc + requirePercentage(taxType) }
        if (ratePercentageSum.signum() == 0) {
            return includedPercentageTaxTypes.associate { it.orgJurisdictionTaxTypeId to Decimals.MoneyScale.round(BigDecimal.ZERO) }
        }
        val leadingTaxTypes = includedPercentageTaxTypes.dropLast(1)
        val leadingAmounts = leadingTaxTypes.map { taxType ->
            Decimals.MoneyScale.divide(includedPercentageTotal * requirePercentage(taxType), ratePercentageSum)
        }
        val remainderAmount = includedPercentageTotal - leadingAmounts.fold(BigDecimal.ZERO, BigDecimal::add)
        return (leadingTaxTypes + includedPercentageTaxTypes.last()).map { it.orgJurisdictionTaxTypeId }
            .zip(leadingAmounts + remainderAmount)
            .toMap()
    }

    private fun taxAmountFor(
        activeTaxType: ActiveTaxTypeForCalculation,
        resolvedTaxableBase: BigDecimal,
        includedPercentageTaxAmounts: Map<UUID, BigDecimal>
    ): BigDecimal = when {
        activeTaxType.calculationMethod == TaxCalculationMethod.FIXED_VALUE -> flatAmountOf(activeTaxType)
        activeTaxType.activeTaxRateDto.taxIsIncludedInTaxableAmount ->
            includedPercentageTaxAmounts.getValue(activeTaxType.orgJurisdictionTaxTypeId)
        else -> Decimals.MoneyScale.divide(resolvedTaxableBase * requirePercentage(activeTaxType), BigDecimal(100))
    }

    private fun flatAmountOf(activeTaxType: ActiveTaxTypeForCalculation): BigDecimal =
        Decimals.MoneyScale.round(requireFlatAmount(activeTaxType))

    private fun requirePercentage(activeTaxType: ActiveTaxTypeForCalculation): BigDecimal =
        activeTaxType.activeTaxRateDto.ratePercentage ?: throw LibertyGenericException("Tax rate missing ratePercentage")

    private fun requireFlatAmount(activeTaxType: ActiveTaxTypeForCalculation): BigDecimal =
        activeTaxType.activeTaxRateDto.rateFlatAmount ?: throw LibertyGenericException("Tax rate missing rateFlatAmount")
}
