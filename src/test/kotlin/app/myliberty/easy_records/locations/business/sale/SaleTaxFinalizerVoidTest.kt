package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxDirection
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxEntryCreateDto
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxEntryDto
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxEntryService
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxSourceType
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeFetcher
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.api.TaxRateService
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeFetcher
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxCalculationMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class SaleTaxFinalizerVoidTest {

    private val taxEntryService = mock(TaxEntryService::class.java)
    private val fiscalPeriodService = mock(FiscalPeriodService::class.java)
    private val saleTaxFinalizer = SaleTaxFinalizer(
        saleRepository = mock(SaleRepository::class.java),
        taxEntryService = taxEntryService,
        orgJurisdictionTaxTypeFetcher = mock(OrgJurisdictionTaxTypeFetcher::class.java),
        jurisdictionTaxTypeFetcher = mock(JurisdictionTaxTypeFetcher::class.java),
        taxRateService = mock(TaxRateService::class.java),
        fiscalPeriodService = fiscalPeriodService,
    )
    private val firstVoidDate = LocalDate.of(2026, 11, 2)
    private val secondVoidDate = LocalDate.of(2026, 11, 3)
    private val firstPeriodId = UUID.randomUUID()
    private val secondPeriodId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SessionContextProvider.setSession(
            SessionContext(
                systemUserId = UUID.randomUUID(),
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = UUID.randomUUID(), schemaName = "loc-1"),
            )
        )
        `when`(fiscalPeriodService.requireOpenForDate(firstVoidDate)).thenReturn(firstPeriodId)
        `when`(fiscalPeriodService.requireOpenForDate(secondVoidDate)).thenReturn(secondPeriodId)
    }

    private fun voidedSale(reference: String, voidDate: LocalDate): VoidedSale {
        val sale = SaleEntity(contactId = UUID.randomUUID(), status = SaleStatus.VOIDED)
        sale.id = UUID.randomUUID()
        sale.referenceNumber = reference
        val saleVoidEntity = SaleVoidEntity(sale.id!!, "reason")
        saleVoidEntity.createdOn = voidDate.atTime(12, 0).atOffset(ZoneOffset.UTC)
        return VoidedSale(sale, saleVoidEntity)
    }

    private fun originalTaxEntry(reference: String) = TaxEntryDto(
        sourceReferenceNumber = reference,
        sourceType = TaxSourceType.SALE,
        direction = TaxDirection.OUTPUT,
        taxTypeId = UUID.randomUUID(),
        taxTypeName = "VAT",
        jurisdictionName = "Kenya",
        fiscalPeriodId = UUID.randomUUID(),
        calculationMethod = TaxCalculationMethod.PERCENTAGE,
        rate = BigDecimal("16"),
        taxIsBilledToCustomerSeparately = true,
        taxIsIncludedInTaxableAmount = false,
        taxableAmount = BigDecimal("100.00"),
        resolvedTaxableBase = BigDecimal("100.00"),
        taxAmount = BigDecimal("16.00"),
    )

    @Test
    fun `reversals are negated, put in the period of their own void date and created in one call`() {
        val firstSale = voidedSale("SALE01", firstVoidDate)
        val secondSale = voidedSale("SALE02", secondVoidDate)
        val thirdSale = voidedSale("SALE03", firstVoidDate)
        val voidedSales = listOf(firstSale, secondSale, thirdSale)
        `when`(taxEntryService.findBySourceReferences(setOf("SALE01", "SALE02", "SALE03"), TaxSourceType.SALE))
            .thenReturn(listOf(originalTaxEntry("SALE01"), originalTaxEntry("SALE02"), originalTaxEntry("SALE03")))

        saleTaxFinalizer.finalizeTaxForVoids(voidedSales)

        verify(fiscalPeriodService, times(1)).requireOpenForDate(firstVoidDate)
        verify(fiscalPeriodService, times(1)).requireOpenForDate(secondVoidDate)
        @Suppress("UNCHECKED_CAST")
        val reversalCaptor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<TaxEntryCreateDto>>
        verify(taxEntryService, times(1)).createAll(capture(reversalCaptor))
        val reversals = reversalCaptor.value
        assertEquals(listOf("SALE01", "SALE02", "SALE03"), reversals.map { it.sourceReferenceNumber })
        assertEquals(listOf(firstPeriodId, secondPeriodId, firstPeriodId), reversals.map { it.fiscalPeriodId })
        assertEquals(TaxSourceType.SALE_VOID, reversals.first().sourceType)
        assertEquals(0, BigDecimal("-16.00").compareTo(reversals.first().taxAmount))
        assertEquals(0, BigDecimal("-100.00").compareTo(reversals.first().taxableAmount))
    }

    @Test
    fun `a void of sales that carried no tax asks for no period and creates nothing`() {
        `when`(taxEntryService.findBySourceReferences(setOf("SALE01"), TaxSourceType.SALE)).thenReturn(emptyList())

        saleTaxFinalizer.finalizeTaxForVoids(listOf(voidedSale("SALE01", firstVoidDate)))

        verifyNoInteractions(fiscalPeriodService)
        verify(taxEntryService, never()).createAll(emptyList())
    }

    private fun <T> capture(captor: ArgumentCaptor<T>): T {
        captor.capture()
        @Suppress("UNCHECKED_CAST")
        return null as T
    }
}
