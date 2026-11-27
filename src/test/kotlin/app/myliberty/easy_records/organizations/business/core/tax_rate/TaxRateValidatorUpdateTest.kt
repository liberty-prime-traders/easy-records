package app.myliberty.easy_records.organizations.business.core.tax_rate

import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeDto
import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeFetcher
import app.myliberty.easy_records.organizations.business.core.tax_rate.api.TaxRateUpdateDto
import app.myliberty.easy_records.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeFetcher
import app.myliberty.easy_records.platform.business.tax_type.api.TaxApplicationLevel
import app.myliberty.easy_records.support.WithDefaultOrganizationSession
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

@WithDefaultOrganizationSession
class TaxRateValidatorUpdateTest {

    private val orgJurisdictionTaxTypeId = UUID.randomUUID()
    private val jurisdictionTaxTypeId = UUID.randomUUID()
    private val orgJurisdictionTaxTypeFetcher = mock(OrgJurisdictionTaxTypeFetcher::class.java)
    private val jurisdictionTaxTypeFetcher = mock(JurisdictionTaxTypeFetcher::class.java)
    private val taxRateCache = mock(TaxRateCache::class.java)
    private val taxRateValidator = TaxRateValidator(orgJurisdictionTaxTypeFetcher, jurisdictionTaxTypeFetcher, taxRateCache)

    private val farFuture = LocalDate.now().plusYears(5)
    private val editedRate = taxRate(name = "State", startDate = LocalDate.now().minusYears(1), endDate = null)
    private val laterRate = taxRate(name = "County", startDate = farFuture.minusYears(1), endDate = null)

    @Test
    fun `an end date that reaches a transaction level rate of another name is refused`() {
        stubTaxApplicationLevel(TaxApplicationLevel.TRANSACTION)
        stubCacheHolding(editedRate, laterRate)

        val refusal = assertThrows(LibertyGenericException::class.java) {
            taxRateValidator.validateForUpdate(editedRate, endDateUpdate(farFuture))
        }

        assertEquals(
            "'County' starts on ${laterRate.startDate} and never expires. Please adjust your dates to avoid this overlap.",
            refusal.message
        )
    }

    @Test
    fun `an end date that stops before the next transaction level rate is accepted`() {
        stubTaxApplicationLevel(TaxApplicationLevel.TRANSACTION)
        stubCacheHolding(editedRate, laterRate)

        assertDoesNotThrow { taxRateValidator.validateForUpdate(editedRate, endDateUpdate(farFuture.minusYears(2))) }
    }

    @Test
    fun `an update that changes neither name nor end date skips the overlap check`() {
        taxRateValidator.validateForUpdate(editedRate, TaxRateUpdateDto(id = editedRate.id))

        verifyNoInteractions(taxRateCache)
    }

    private fun endDateUpdate(endDate: LocalDate) = TaxRateUpdateDto(id = editedRate.id, endDate = Optional.of(endDate))

    private fun stubCacheHolding(vararg taxRateDtos: TaxRateDto) {
        `when`(taxRateCache.getAll()).thenReturn(taxRateDtos.toList())
    }

    private fun stubTaxApplicationLevel(taxApplicationLevel: TaxApplicationLevel) {
        val parent = OrgJurisdictionTaxTypeDto(
            id = orgJurisdictionTaxTypeId,
            createdById = UUID.randomUUID(),
            createdOn = OffsetDateTime.now(),
            referenceNumber = "OJTT000001",
            jurisdictionTaxTypeId = jurisdictionTaxTypeId,
            payableAccountCode = "2100",
        )
        `when`(orgJurisdictionTaxTypeFetcher.getAllDtos()).thenReturn(listOf(parent))
        `when`(jurisdictionTaxTypeFetcher.getTaxApplicationLevel(jurisdictionTaxTypeId)).thenReturn(taxApplicationLevel)
    }

    private fun taxRate(name: String, startDate: LocalDate, endDate: LocalDate?) = TaxRateDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        referenceNumber = "TR000001",
        orgJurisdictionTaxTypeId = orgJurisdictionTaxTypeId,
        name = name,
        taxIsBilledToCustomerSeparately = true,
        taxIsIncludedInTaxableAmount = false,
        startDate = startDate,
        endDate = endDate,
    )
}
