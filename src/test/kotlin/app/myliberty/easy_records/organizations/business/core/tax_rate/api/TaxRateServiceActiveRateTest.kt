package app.myliberty.easy_records.organizations.business.core.tax_rate.api

import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeFetcher
import app.myliberty.easy_records.organizations.business.core.tax_rate.TaxRateCache
import app.myliberty.easy_records.organizations.business.core.tax_rate.TaxRateEntity
import app.myliberty.easy_records.organizations.business.core.tax_rate.TaxRateMapper
import app.myliberty.easy_records.organizations.business.core.tax_rate.TaxRateRepository
import app.myliberty.easy_records.organizations.business.core.tax_rate.TaxRateValidator
import app.myliberty.easy_records.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeFetcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class TaxRateServiceActiveRateTest {

    private val taxRateRepository = mock(TaxRateRepository::class.java)
    private val taxRateService = TaxRateService(
        taxRateMapper = mock(TaxRateMapper::class.java),
        taxRateCache = mock(TaxRateCache::class.java),
        taxRateValidator = mock(TaxRateValidator::class.java),
        orgJurisdictionTaxTypeFetcher = mock(OrgJurisdictionTaxTypeFetcher::class.java),
        jurisdictionTaxTypeFetcher = mock(JurisdictionTaxTypeFetcher::class.java),
        taxRateRepository = taxRateRepository
    )
    private val calculationDate = LocalDate.of(2026, 6, 15)

    @Test
    fun `each active rate is keyed by its org jurisdiction tax type and keeps its billing flags`() {
        val percentageParentId = UUID.randomUUID()
        val flatParentId = UUID.randomUUID()
        val percentageRate = taxRateEntity(percentageParentId, ratePercentage = BigDecimal("7.50"))
        val flatRate = taxRateEntity(flatParentId, rateFlatAmount = BigDecimal("2.0000"), billedSeparately = true)
        `when`(taxRateRepository.findActiveRateByDate(calculationDate)).thenReturn(listOf(percentageRate, flatRate))

        val activeRates = taxRateService.findActiveRateForDate(calculationDate)

        assertEquals(setOf(percentageParentId, flatParentId), activeRates.keys)
        assertEquals(
            ActiveTaxRateDto(
                ratePercentage = BigDecimal("7.50"),
                rateFlatAmount = null,
                taxIsBilledToCustomerSeparately = false,
                taxIsIncludedInTaxableAmount = true
            ),
            activeRates.getValue(percentageParentId)
        )
        assertEquals(
            ActiveTaxRateDto(
                ratePercentage = null,
                rateFlatAmount = BigDecimal("2.0000"),
                taxIsBilledToCustomerSeparately = true,
                taxIsIncludedInTaxableAmount = true
            ),
            activeRates.getValue(flatParentId)
        )
    }

    @Test
    fun `no active rate on the date gives an empty map`() {
        `when`(taxRateRepository.findActiveRateByDate(calculationDate)).thenReturn(emptyList())

        assertTrue(taxRateService.findActiveRateForDate(calculationDate).isEmpty())
    }

    private fun taxRateEntity(
        orgJurisdictionTaxTypeId: UUID,
        ratePercentage: BigDecimal? = null,
        rateFlatAmount: BigDecimal? = null,
        billedSeparately: Boolean = false
    ) = TaxRateEntity(
        orgJurisdictionTaxTypeId = orgJurisdictionTaxTypeId,
        name = "Levy",
        ratePercentage = ratePercentage,
        rateFlatAmount = rateFlatAmount,
        taxIsBilledToCustomerSeparately = billedSeparately,
        taxIsIncludedInTaxableAmount = true,
        startDate = LocalDate.of(2026, 1, 1)
    )
}
