package app.myliberty.easy_records.locations.business.retail.sale_session

import app.myliberty.easy_records.configuration.session.LocationSession
import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductService
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleDataFetcher
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleHeaderDto
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleLineDto
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleStatus
import app.myliberty.easy_records.locations.business.retail.sale_adjustment.api.PriceAdjustmentMethod
import app.myliberty.easy_records.locations.business.retail.sale_adjustment.api.SaleAdjustmentDto
import app.myliberty.easy_records.locations.business.retail.sale_adjustment.api.SaleAdjustmentFetcher
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSession
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionAdjustment
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SessionIdentity
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentDirection
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentReasonService
import app.myliberty.easy_records.support.WithDefaultOrganizationSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

@WithDefaultOrganizationSession
class SaleSessionCopierContentTest {

    private val saleDataFetcher = mock(SaleDataFetcher::class.java)
    private val saleAdjustmentFetcher = mock(SaleAdjustmentFetcher::class.java)
    // Stands in for the generated mapper: an adjustment is related to the identity the per-sale map holds for its line.
    private val domainToSessionMapper = mock(DomainToSessionMapper::class.java) { invocation ->
        when (invocation.method.name) {
            "toSaleSessionLine" -> toSessionLine(invocation.getArgument(0))
            "toSaleSessionAdjustment" -> toSessionAdjustment(invocation.getArgument(0), invocation.getArgument(1))
            else -> null
        }
    }
    private val saleSessionCopier = SaleSessionCopier(
        saleDataFetcher,
        saleAdjustmentFetcher,
        domainToSessionMapper,
        mock(SaleSessionStockOverlay::class.java) { invocation -> invocation.getArgument<SaleSession>(0) },
        SaleSessionValidator(mock(AdjustmentReasonService::class.java), mock(LocationProductService::class.java)),
    )

    private val contactId = UUID.randomUUID()
    private val firstProductId = UUID.randomUUID()
    private val secondProductId = UUID.randomUUID()

    @BeforeEach
    fun setUserAndLocationSession() {
        SessionContextProvider.setSession(
            SessionContext(
                systemUserId = UUID.randomUUID(),
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = UUID.randomUUID(), schemaName = "loc-1"),
            )
        )
    }

    @Test
    fun `each copied adjustment points at the copy of its own sale's line and carries fresh identities`() {
        val firstSaleHeader = saleHeader("SALE01")
        val secondSaleHeader = saleHeader("SALE02")
        val firstSaleLine = saleLineDto(firstProductId, "20.00")
        val secondSaleLine = saleLineDto(secondProductId, "50.00")
        val firstSaleLineDiscount = adjustmentDto(firstSaleLine.id, "10")
        val secondSaleLineDiscount = adjustmentDto(secondSaleLine.id, "5")
        val firstSaleOrderDiscount = adjustmentDto(null, "2")
        val sourceSaleIds = listOf(firstSaleHeader.id, secondSaleHeader.id)
        `when`(saleDataFetcher.getSaleLinesBySaleId(sourceSaleIds)).thenReturn(
            mapOf(firstSaleHeader.id to listOf(firstSaleLine), secondSaleHeader.id to listOf(secondSaleLine))
        )
        `when`(saleAdjustmentFetcher.getAdjustmentsBySaleId(sourceSaleIds)).thenReturn(
            mapOf(
                firstSaleHeader.id to listOf(firstSaleLineDiscount, firstSaleOrderDiscount),
                secondSaleHeader.id to listOf(secondSaleLineDiscount),
            )
        )

        val consolidatedSession = saleSessionCopier.copyFromSalesWithSourceReferences(
            UUID.randomUUID(), listOf(firstSaleHeader, secondSaleHeader)
        ).saleSession

        val copiedFirstLine = consolidatedSession.saleLines.single { it.locationProductId == firstProductId }
        val copiedSecondLine = consolidatedSession.saleLines.single { it.locationProductId == secondProductId }
        val copiedFirstDiscount = consolidatedSession.saleAdjustments
            .single { it.value.compareTo(BigDecimal("10")) == 0 }
        val copiedSecondDiscount = consolidatedSession.saleAdjustments
            .single { it.value.compareTo(BigDecimal("5")) == 0 }
        val copiedOrderDiscount = consolidatedSession.saleAdjustments
            .single { it.value.compareTo(BigDecimal("2")) == 0 }
        assertEquals(copiedFirstLine.identity, copiedFirstDiscount.relatedSaleLineIdentity)
        assertEquals(copiedSecondLine.identity, copiedSecondDiscount.relatedSaleLineIdentity)
        assertNull(copiedOrderDiscount.relatedSaleLineIdentity)
        val everyCopiedIdentity = consolidatedSession.saleLines.map { it.identity } +
            consolidatedSession.saleAdjustments.map { it.identity }
        assertTrue(everyCopiedIdentity.all { !it.isPersisted() })
        assertEquals(everyCopiedIdentity.size, everyCopiedIdentity.toSet().size)
    }

    @Test
    fun `a single sale is copied with its contact, notes, lines and adjustments under fresh identities`() {
        val sourceSaleHeader = saleHeader("SALE01", notes = "deliver Monday")
        val sourceLine = saleLineDto(firstProductId, "20.00")
        val sourceDiscount = adjustmentDto(sourceLine.id, "10")
        `when`(saleDataFetcher.getSaleHeader(sourceSaleHeader.id)).thenReturn(sourceSaleHeader)
        `when`(saleDataFetcher.getSaleLines(sourceSaleHeader.id)).thenReturn(listOf(sourceLine))
        `when`(saleAdjustmentFetcher.getAdjustments(sourceSaleHeader.id)).thenReturn(listOf(sourceDiscount))

        val copiedSession = saleSessionCopier.copyFromSale(UUID.randomUUID(), sourceSaleHeader.id)

        val copiedLine = copiedSession.saleLines.single()
        val copiedDiscount = copiedSession.saleAdjustments.single()
        assertEquals(contactId, copiedSession.header.contactId)
        assertEquals("deliver Monday", copiedSession.header.notes)
        assertNull(copiedSession.header.referenceNumber)
        assertFalse(copiedLine.identity.isPersisted())
        assertEquals(copiedLine.identity, copiedDiscount.relatedSaleLineIdentity)
    }

    @Test
    fun `a consolidation ranks products alphabetically and puts the lines of one product on hundredths`() {
        val zincLine = saleLineDto(firstProductId, "20.00", "zinc")
        val appleLine = saleLineDto(secondProductId, "50.00", "Apple")
        val secondZincLine = saleLineDto(firstProductId, "10.00", "zinc")
        val saleHeaders = listOf(saleHeader("SALE01"), saleHeader("SALE02"), saleHeader("SALE03"))
        val sourceSaleIds = saleHeaders.map { it.id }
        `when`(saleDataFetcher.getSaleLinesBySaleId(sourceSaleIds)).thenReturn(
            mapOf(
                saleHeaders[0].id to listOf(zincLine),
                saleHeaders[1].id to listOf(appleLine),
                saleHeaders[2].id to listOf(secondZincLine),
            )
        )
        `when`(saleAdjustmentFetcher.getAdjustmentsBySaleId(sourceSaleIds)).thenReturn(
            saleHeaders.associate { it.id to emptyList() }
        )

        val consolidatedSession = saleSessionCopier
            .copyFromSalesWithSourceReferences(UUID.randomUUID(), saleHeaders).saleSession

        assertEquals(
            listOf("Apple" to "1.00", "zinc" to "2.00", "zinc" to "2.01"),
            consolidatedSession.saleLines.sortedBy { it.rank }.map { it.productLabel to it.rank.toPlainString() }
        )
        assertEquals(
            listOf(BigDecimal("20.00"), BigDecimal("10.00")),
            consolidatedSession.saleLines.filter { it.productLabel == "zinc" }.sortedBy { it.rank }.map { it.lineTotal }
        )
    }

    @Test
    fun `a single sale is copied with its lines ranked alphabetically`() {
        val sourceSaleHeader = saleHeader("SALE01")
        val sourceLines = listOf(
            saleLineDto(firstProductId, "20.00", "Rice"), saleLineDto(secondProductId, "20.00", "beans")
        )
        `when`(saleDataFetcher.getSaleHeader(sourceSaleHeader.id)).thenReturn(sourceSaleHeader)
        `when`(saleDataFetcher.getSaleLines(sourceSaleHeader.id)).thenReturn(sourceLines)
        `when`(saleAdjustmentFetcher.getAdjustments(sourceSaleHeader.id)).thenReturn(emptyList())

        val copiedSession = saleSessionCopier.copyFromSale(UUID.randomUUID(), sourceSaleHeader.id)

        assertEquals(
            listOf("beans" to BigDecimal(1), "Rice" to BigDecimal(2)),
            copiedSession.saleLines.sortedBy { it.rank }.map { it.productLabel to it.rank }
        )
    }

    private fun saleHeader(referenceNumber: String, notes: String? = null) = SaleHeaderDto(
        id = UUID.randomUUID(),
        referenceNumber = referenceNumber,
        version = 0,
        status = SaleStatus.DRAFT,
        contactId = contactId,
        soldById = null,
        dateSold = null,
        notes = notes,
    )

    private fun saleLineDto(locationProductId: UUID, lineTotal: String, productLabel: String = "Product") = SaleLineDto(
        id = UUID.randomUUID(),
        locationProductId = locationProductId,
        productLabel = productLabel,
        productPrintLabel = productLabel,
        userProvidedQuantity = BigDecimal("2"),
        unitId = UUID.randomUUID(),
        baseUnitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 1,
        lineTotal = BigDecimal(lineTotal),
        defaultSalePrice = BigDecimal("10.00"),
    )

    private fun adjustmentDto(saleLineId: UUID?, percentage: String) = SaleAdjustmentDto(
        id = UUID.randomUUID(),
        saleLineId = saleLineId,
        adjustmentReasonId = UUID.randomUUID(),
        direction = AdjustmentDirection.DISCOUNT,
        calculationMethod = PriceAdjustmentMethod.PERCENTAGE,
        value = BigDecimal(percentage),
        note = null,
        approvedById = null,
    )

    private fun toSessionLine(saleLineDto: SaleLineDto) = SaleSessionLine(
        identity = SessionIdentity.persisted(saleLineDto.id),
        locationProductId = saleLineDto.locationProductId,
        productLabel = saleLineDto.productLabel,
        productPrintLabel = saleLineDto.productPrintLabel,
        userProvidedQuantity = saleLineDto.userProvidedQuantity,
        unitId = saleLineDto.unitId,
        baseUnitId = saleLineDto.baseUnitId,
        storageUnitsPerUserProvidedUnit = saleLineDto.storageUnitsPerUserProvidedUnit,
        lineTotal = saleLineDto.lineTotal,
        defaultSalePrice = saleLineDto.defaultSalePrice,
    )

    private fun toSessionAdjustment(
        saleAdjustmentDto: SaleAdjustmentDto,
        relatedSaleLineIdentityBySaleLineId: Map<UUID, SessionIdentity>,
    ) = SaleSessionAdjustment(
        identity = SessionIdentity.persisted(saleAdjustmentDto.id),
        relatedSaleLineIdentity = saleAdjustmentDto.saleLineId?.let { relatedSaleLineIdentityBySaleLineId[it] },
        adjustmentReasonId = saleAdjustmentDto.adjustmentReasonId,
        direction = saleAdjustmentDto.direction,
        calculationMethod = saleAdjustmentDto.calculationMethod,
        value = saleAdjustmentDto.value,
        note = saleAdjustmentDto.note,
        approvedById = saleAdjustmentDto.approvedById,
    )
}
