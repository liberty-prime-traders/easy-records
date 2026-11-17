package app.myliberty.easy_records.organizations.business.core.tax_rate

import app.myliberty.easy_records.support.WithDefaultOrganizationSession
import app.myliberty.easy_records.organizations.business.core.tax_rate.api.TaxRateInsertDto
import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.business.mappers.MoneyQualifier
import app.myliberty.easy_records.util.business.mappers.StringQualifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@WithDefaultOrganizationSession
class TaxRateMapperMoneyTest {

    private val taxRateMapper = TaxRateMapperImpl(StringQualifier, MoneyQualifier)

    @Test
    fun `the flat tax amount reaches the entity as the typed value`() {
        val taxRateInsertDto = TaxRateInsertDto(
            orgJurisdictionTaxTypeId = UUID.randomUUID(),
            name = "Levy",
            rateFlatAmount = Money.of(BigDecimal("12.75")),
            taxIsBilledToCustomerSeparately = false,
            taxIsIncludedInTaxableAmount = false,
            startDate = LocalDate.of(2026, 1, 1),
        )
        assertEquals(BigDecimal("12.75"), taxRateMapper.toEntity(taxRateInsertDto).rateFlatAmount)
    }
}
