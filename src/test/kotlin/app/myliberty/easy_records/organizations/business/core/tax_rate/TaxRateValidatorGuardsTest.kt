package app.myliberty.easy_records.organizations.business.core.tax_rate

import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeDto
import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeFetcher
import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeStatus
import app.myliberty.easy_records.organizations.business.core.tax_rate.api.TaxRateInsertDto
import app.myliberty.easy_records.organizations.business.core.tax_rate.api.TaxRateUpdateDto
import app.myliberty.easy_records.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeFetcher
import app.myliberty.easy_records.platform.business.tax_type.api.TaxApplicationLevel
import app.myliberty.easy_records.platform.business.tax_type.api.TaxCalculationMethod
import app.myliberty.easy_records.support.WithDefaultOrganizationSession
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

@WithDefaultOrganizationSession
class TaxRateValidatorGuardsTest {

    private val orgJurisdictionTaxTypeId = UUID.randomUUID()
    private val jurisdictionTaxTypeId = UUID.randomUUID()
    private val orgJurisdictionTaxTypeFetcher = mock(OrgJurisdictionTaxTypeFetcher::class.java)
    private val jurisdictionTaxTypeFetcher = mock(JurisdictionTaxTypeFetcher::class.java)
    private val taxRateCache = mock(TaxRateCache::class.java)
    private val taxRateValidator = TaxRateValidator(orgJurisdictionTaxTypeFetcher, jurisdictionTaxTypeFetcher, taxRateCache)

    private val today: LocalDate
        get() = DateTimes.Local.Now.organization()

    @Test
    fun `a blank name is refused on create`() {
        stubParent(TaxApplicationLevel.TRANSACTION, TaxCalculationMethod.PERCENTAGE)

        assertRefused("Name must not be blank") { taxRateValidator.validateForCreate(insertDto(name = " ")) }
    }

    @Test
    fun `a rate that is both billed separately and included in the taxable amount is refused`() {
        stubParent(TaxApplicationLevel.TRANSACTION, TaxCalculationMethod.PERCENTAGE)

        assertRefused("A tax already included in the taxable amount cannot also be billed separately") {
            taxRateValidator.validateForCreate(insertDto(billedSeparately = true, includedInTaxableAmount = true))
        }
    }

    @Test
    fun `an end date in the past is refused on create`() {
        stubParent(TaxApplicationLevel.TRANSACTION, TaxCalculationMethod.PERCENTAGE)

        assertRefused("End date must not be in the past") {
            taxRateValidator.validateForCreate(insertDto(startDate = today.minusDays(10), endDate = today.minusDays(1)))
        }
    }

    @Test
    fun `an end date of today is accepted on create`() {
        stubParent(TaxApplicationLevel.TRANSACTION, TaxCalculationMethod.PERCENTAGE)
        stubCacheHolding()

        assertDoesNotThrow {
            taxRateValidator.validateForCreate(insertDto(startDate = today, endDate = today))
        }
    }

    @Test
    fun `a start date after the end date is refused on create`() {
        stubParent(TaxApplicationLevel.TRANSACTION, TaxCalculationMethod.PERCENTAGE)

        assertRefused("The start date must be before the end date") {
            taxRateValidator.validateForCreate(insertDto(startDate = today.plusDays(10), endDate = today.plusDays(5)))
        }
    }

    @Test
    fun `a parent tax type that does not exist is refused`() {
        `when`(orgJurisdictionTaxTypeFetcher.getAllDtos()).thenReturn(emptyList())

        assertRefused("Org jurisdiction tax type not found") { taxRateValidator.validateForCreate(insertDto()) }
    }

    @Test
    fun `an inactive parent tax type is refused`() {
        stubParent(TaxApplicationLevel.TRANSACTION, TaxCalculationMethod.PERCENTAGE, OrgJurisdictionTaxTypeStatus.INACTIVE)

        assertRefused("Cannot create a rate for an inactive Tax Type") {
            taxRateValidator.validateForCreate(insertDto())
        }
    }

    @Test
    fun `a percentage tax type requires a percentage`() {
        stubParent(TaxApplicationLevel.TRANSACTION, TaxCalculationMethod.PERCENTAGE)

        assertRefused("ratePercentage is required for PERCENTAGE tax types") {
            taxRateValidator.validateForCreate(insertDto(ratePercentage = null))
        }
    }

    @Test
    fun `a negative percentage is refused`() {
        stubParent(TaxApplicationLevel.TRANSACTION, TaxCalculationMethod.PERCENTAGE)

        assertRefused("ratePercentage must be not be negative") {
            taxRateValidator.validateForCreate(insertDto(ratePercentage = BigDecimal("-0.01")))
        }
    }

    @Test
    fun `a percentage above one hundred is refused and one hundred itself is accepted`() {
        stubParent(TaxApplicationLevel.TRANSACTION, TaxCalculationMethod.PERCENTAGE)
        stubCacheHolding()

        assertRefused("ratePercentage must not exceed 100") {
            taxRateValidator.validateForCreate(insertDto(ratePercentage = BigDecimal("100.01")))
        }
        assertDoesNotThrow { taxRateValidator.validateForCreate(insertDto(ratePercentage = BigDecimal("100"))) }
        assertDoesNotThrow { taxRateValidator.validateForCreate(insertDto(ratePercentage = BigDecimal.ZERO)) }
    }

    @Test
    fun `a flat amount is refused on a percentage tax type`() {
        stubParent(TaxApplicationLevel.TRANSACTION, TaxCalculationMethod.PERCENTAGE)

        assertRefused("rateFlatAmount must not be provided for PERCENTAGE tax types") {
            taxRateValidator.validateForCreate(insertDto(rateFlatAmount = Money.of(BigDecimal("1.00"))))
        }
    }

    @Test
    fun `a fixed value tax type requires a flat amount`() {
        stubParent(TaxApplicationLevel.TRANSACTION, TaxCalculationMethod.FIXED_VALUE)

        assertRefused("rateFlatAmount is required for FIXED_VALUE tax types") {
            taxRateValidator.validateForCreate(insertDto(ratePercentage = null, rateFlatAmount = null))
        }
    }

    @Test
    fun `a percentage is refused on a fixed value tax type`() {
        stubParent(TaxApplicationLevel.TRANSACTION, TaxCalculationMethod.FIXED_VALUE)

        assertRefused("ratePercentage must not be provided for FIXED_VALUE tax types") {
            taxRateValidator.validateForCreate(
                insertDto(ratePercentage = BigDecimal("5"), rateFlatAmount = Money.of(BigDecimal("1.00")))
            )
        }
    }

    @Test
    fun `a flat amount alone is accepted on a fixed value tax type`() {
        stubParent(TaxApplicationLevel.TRANSACTION, TaxCalculationMethod.FIXED_VALUE)
        stubCacheHolding()

        assertDoesNotThrow {
            taxRateValidator.validateForCreate(
                insertDto(ratePercentage = null, rateFlatAmount = Money.of(BigDecimal("1.00")))
            )
        }
    }

    @Test
    fun `an open ended rate overlapping an expiring transaction level rate names its expiry`() {
        stubParent(TaxApplicationLevel.TRANSACTION, TaxCalculationMethod.PERCENTAGE)
        val expiringRate = existingRate("Other", today.minusDays(5), today.plusDays(30))
        stubCacheHolding(expiringRate)

        assertRefused("'Other' starts on ${expiringRate.startDate} and expires on ${expiringRate.endDate}. " +
            "Please adjust your dates to avoid this overlap.") {
            taxRateValidator.validateForCreate(insertDto(startDate = today, endDate = null))
        }
    }

    @Test
    fun `an open ended rate overlapping an open ended transaction level rate says it never expires`() {
        stubParent(TaxApplicationLevel.TRANSACTION, TaxCalculationMethod.PERCENTAGE)
        val openRate = existingRate("Other", today.minusDays(5), null)
        stubCacheHolding(openRate)

        assertRefused("'Other' starts on ${openRate.startDate} and never expires. " +
            "Please adjust your dates to avoid this overlap.") {
            taxRateValidator.validateForCreate(insertDto(startDate = today, endDate = null))
        }
    }

    @Test
    fun `a rate that expired before the new start does not overlap`() {
        stubParent(TaxApplicationLevel.TRANSACTION, TaxCalculationMethod.PERCENTAGE)
        stubCacheHolding(existingRate("Old", today.minusDays(30), today.minusDays(1)))

        assertDoesNotThrow { taxRateValidator.validateForCreate(insertDto(startDate = today, endDate = null)) }
    }

    @Test
    fun `a rate that ends on the day another starts overlaps it`() {
        stubParent(TaxApplicationLevel.TRANSACTION, TaxCalculationMethod.PERCENTAGE)
        stubCacheHolding(existingRate("Later", today.plusDays(10), null))

        assertThrows(LibertyGenericException::class.java) {
            taxRateValidator.validateForCreate(insertDto(startDate = today, endDate = today.plusDays(10)))
        }
        assertDoesNotThrow {
            taxRateValidator.validateForCreate(insertDto(startDate = today, endDate = today.plusDays(9)))
        }
    }

    @Test
    fun `a rate under another tax type never overlaps`() {
        stubParent(TaxApplicationLevel.TRANSACTION, TaxCalculationMethod.PERCENTAGE)
        stubCacheHolding(existingRate("Other", today.minusDays(5), null, UUID.randomUUID()))

        assertDoesNotThrow { taxRateValidator.validateForCreate(insertDto()) }
    }

    @Test
    fun `a line item rate only conflicts with an equivalent name`() {
        stubParent(TaxApplicationLevel.LINE_ITEM, TaxCalculationMethod.PERCENTAGE)
        stubCacheHolding(existingRate("State Tax", today.minusDays(5), null))

        assertThrows(LibertyGenericException::class.java) {
            taxRateValidator.validateForCreate(insertDto(name = "state  tax"))
        }
        assertDoesNotThrow { taxRateValidator.validateForCreate(insertDto(name = "County Tax")) }
    }

    @Test
    fun `a null name update is refused`() {
        val editedRate = existingRate("State", today.minusDays(5), null)

        assertRefused("Name must not be null") {
            taxRateValidator.validateForUpdate(editedRate, TaxRateUpdateDto(id = editedRate.id, name = Optional.empty()))
        }
    }

    @Test
    fun `a null end date update is refused`() {
        val editedRate = existingRate("State", today.minusDays(5), null)

        assertRefused("End date must not be null") {
            taxRateValidator.validateForUpdate(
                editedRate,
                TaxRateUpdateDto(id = editedRate.id, endDate = Optional.empty())
            )
        }
    }

    @Test
    fun `a blank name update is refused`() {
        val editedRate = existingRate("State", today.minusDays(5), null)

        assertRefused("Name must not be blank") {
            taxRateValidator.validateForUpdate(
                editedRate,
                TaxRateUpdateDto(id = editedRate.id, name = Optional.of(""))
            )
        }
    }

    @Test
    fun `an end date update in the past is refused`() {
        val editedRate = existingRate("State", today.minusDays(5), null)

        assertRefused("End date must not be in the past") {
            taxRateValidator.validateForUpdate(
                editedRate,
                TaxRateUpdateDto(id = editedRate.id, endDate = Optional.of(today.minusDays(1)))
            )
        }
    }

    @Test
    fun `an end date update before the start date is refused`() {
        val editedRate = existingRate("State", today.plusDays(10), null)

        assertRefused("The start date must be before the end date") {
            taxRateValidator.validateForUpdate(
                editedRate,
                TaxRateUpdateDto(id = editedRate.id, endDate = Optional.of(today.plusDays(5)))
            )
        }
    }

    @Test
    fun `a renamed line item rate conflicts with an equivalent name over its existing end date`() {
        stubParent(TaxApplicationLevel.LINE_ITEM, TaxCalculationMethod.PERCENTAGE)
        val editedRate = existingRate("State", today.minusDays(5), today.plusDays(100))
        val overlappingRate = existingRate("County", today.plusDays(50), today.plusDays(60))
        val laterRate = existingRate("County", today.plusDays(200), null)
        stubCacheHolding(editedRate, overlappingRate, laterRate)

        assertRefused("'County' starts on ${overlappingRate.startDate} and expires on ${overlappingRate.endDate}. " +
            "Please adjust your dates to avoid this overlap.") {
            taxRateValidator.validateForUpdate(
                editedRate,
                TaxRateUpdateDto(id = editedRate.id, name = Optional.of("county"))
            )
        }
        assertDoesNotThrow {
            taxRateValidator.validateForUpdate(
                editedRate,
                TaxRateUpdateDto(id = editedRate.id, name = Optional.of("Parish"))
            )
        }
    }

    @Test
    fun `a rate never conflicts with its own stored copy`() {
        stubParent(TaxApplicationLevel.LINE_ITEM, TaxCalculationMethod.PERCENTAGE)
        val editedRate = existingRate("State", today.minusDays(5), null)
        stubCacheHolding(editedRate)

        assertDoesNotThrow {
            taxRateValidator.validateForUpdate(
                editedRate,
                TaxRateUpdateDto(id = editedRate.id, name = Optional.of("State"))
            )
        }
    }

    @Test
    fun `an end date update keeps the current name when checking a line item conflict`() {
        stubParent(TaxApplicationLevel.LINE_ITEM, TaxCalculationMethod.PERCENTAGE)
        val editedRate = existingRate("State", today.minusDays(5), null)
        val sameNameRate = existingRate("State", today.plusDays(50), null)
        val otherNameRate = existingRate("County", today.plusDays(50), null)

        stubCacheHolding(editedRate, otherNameRate)
        assertDoesNotThrow {
            taxRateValidator.validateForUpdate(
                editedRate,
                TaxRateUpdateDto(id = editedRate.id, endDate = Optional.of(today.plusDays(100)))
            )
        }

        stubCacheHolding(editedRate, sameNameRate)
        assertThrows(LibertyGenericException::class.java) {
            taxRateValidator.validateForUpdate(
                editedRate,
                TaxRateUpdateDto(id = editedRate.id, endDate = Optional.of(today.plusDays(100)))
            )
        }
    }

    @Test
    fun `a name only update checks the rate over its stored end date`() {
        stubParent(TaxApplicationLevel.LINE_ITEM, TaxCalculationMethod.PERCENTAGE)
        val editedRate = existingRate("State", today.minusDays(5), today.plusDays(100))
        val rateStartingAfterEditedRateEnds = existingRate("County", today.plusDays(200), null)
        stubCacheHolding(editedRate, rateStartingAfterEditedRateEnds)

        assertDoesNotThrow {
            taxRateValidator.validateForUpdate(
                editedRate,
                TaxRateUpdateDto(id = editedRate.id, name = Optional.of("County"))
            )
        }
    }

    private fun assertRefused(expectedMessage: String, action: () -> Unit) {
        val refusal = assertThrows(LibertyGenericException::class.java) { action() }
        assertEquals(expectedMessage, refusal.message)
    }

    private fun insertDto(
        name: String = "State",
        ratePercentage: BigDecimal? = BigDecimal("5"),
        rateFlatAmount: Money? = null,
        billedSeparately: Boolean = false,
        includedInTaxableAmount: Boolean = false,
        startDate: LocalDate = today,
        endDate: LocalDate? = null
    ) = TaxRateInsertDto(
        orgJurisdictionTaxTypeId = orgJurisdictionTaxTypeId,
        name = name,
        ratePercentage = ratePercentage,
        rateFlatAmount = rateFlatAmount,
        taxIsBilledToCustomerSeparately = billedSeparately,
        taxIsIncludedInTaxableAmount = includedInTaxableAmount,
        startDate = startDate,
        endDate = endDate
    )

    private fun stubCacheHolding(vararg taxRateDtos: TaxRateDto) {
        `when`(taxRateCache.getAll()).thenReturn(taxRateDtos.toList())
    }

    private fun stubParent(
        taxApplicationLevel: TaxApplicationLevel,
        taxCalculationMethod: TaxCalculationMethod,
        status: OrgJurisdictionTaxTypeStatus = OrgJurisdictionTaxTypeStatus.ACTIVE
    ) {
        val parent = OrgJurisdictionTaxTypeDto(
            id = orgJurisdictionTaxTypeId,
            createdById = UUID.randomUUID(),
            createdOn = OffsetDateTime.now(),
            referenceNumber = "OJTT000001",
            jurisdictionTaxTypeId = jurisdictionTaxTypeId,
            status = status,
            payableAccountCode = "2100",
        )
        `when`(orgJurisdictionTaxTypeFetcher.getAllDtos()).thenReturn(listOf(parent))
        `when`(jurisdictionTaxTypeFetcher.getTaxApplicationLevel(jurisdictionTaxTypeId)).thenReturn(taxApplicationLevel)
        `when`(jurisdictionTaxTypeFetcher.getCalculationMethod(jurisdictionTaxTypeId)).thenReturn(taxCalculationMethod)
    }

    private fun existingRate(
        name: String,
        startDate: LocalDate,
        endDate: LocalDate?,
        parentId: UUID = orgJurisdictionTaxTypeId
    ) = TaxRateDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        referenceNumber = "TR000001",
        orgJurisdictionTaxTypeId = parentId,
        name = name,
        taxIsBilledToCustomerSeparately = true,
        taxIsIncludedInTaxableAmount = false,
        startDate = startDate,
        endDate = endDate,
    )
}
