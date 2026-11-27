package app.myliberty.easy_records.organizations.business.core.tax_rate

import app.myliberty.easy_records.organizations.business.core.tax_rate.api.TaxRateInsertDto
import app.myliberty.easy_records.support.WithDefaultOrganizationSession
import app.myliberty.easy_records.util.business.mappers.MoneyQualifier
import app.myliberty.easy_records.util.business.mappers.StringQualifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@WithDefaultOrganizationSession
class TaxRateMapperFlagsTest {

    private val taxRateMapper = TaxRateMapperImpl(StringQualifier, MoneyQualifier)

    @Test
    fun `billing and inclusion flags of both values survive the insert to entity to dto path`() {
        listOf(false to true, true to false).forEach { (billedSeparately, includedInTaxableAmount) ->
            val taxRateInsertDto = TaxRateInsertDto(
                orgJurisdictionTaxTypeId = UUID.randomUUID(),
                name = "Levy",
                ratePercentage = BigDecimal("5"),
                taxIsBilledToCustomerSeparately = billedSeparately,
                taxIsIncludedInTaxableAmount = includedInTaxableAmount,
                startDate = LocalDate.of(2026, 1, 1),
            )

            val taxRateEntity = taxRateMapper.toEntity(taxRateInsertDto)

            assertEquals(billedSeparately, taxRateEntity.taxIsBilledToCustomerSeparately)
            assertEquals(includedInTaxableAmount, taxRateEntity.taxIsIncludedInTaxableAmount)
        }
    }
}
