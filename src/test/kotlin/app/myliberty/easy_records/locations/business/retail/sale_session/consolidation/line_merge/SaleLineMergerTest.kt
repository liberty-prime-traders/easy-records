package app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.line_merge

import app.myliberty.easy_records.locations.business.retail.sale_adjustment.api.PriceAdjustmentMethod
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionAdjustment
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SessionIdentity
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentDirection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import app.myliberty.easy_records.organizations.business.retail.unitconversion.api.UnitConversionGraph
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitPlacement
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import app.myliberty.easy_records.util.quantity.BreakdownUnit
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class SaleLineMergerTest {

    private val sugarId = UUID.randomUUID()
    private val riceId = UUID.randomUUID()
    private val pieceUnitId = UUID.randomUUID()
    private val dozenUnitId = UUID.randomUUID()
    private val storageUnitId = UUID.randomUUID()
    private val adjustmentReasonId = UUID.randomUUID()

    @Test
    fun `products on a single line pass through untouched and keep their order`() {
        val sugarLine = saleLine(sugarId, "3", "45.00")
        val riceLine = saleLine(riceId, "2", "40.00")

        val result = merge(listOf(sugarLine, riceLine))

        assertEquals(listOf(sugarLine, riceLine), result.mergedSaleLines)
        assertTrue(result.problems.isEmpty())
    }

    @Test
    fun `lines of one product in the same unit merge into one line with the summed quantity and total`() {
        val firstSugarLine = saleLine(sugarId, "3", "45.00")
        val riceLine = saleLine(riceId, "2", "40.00")
        val secondSugarLine = saleLine(sugarId, "4", "60.00")

        val result = merge(listOf(firstSugarLine, riceLine, secondSugarLine))

        assertEquals(listOf(sugarId, riceId), result.mergedSaleLines.map { it.locationProductId })
        val mergedSugarLine = result.mergedSaleLines.first()
        assertEquals(BigDecimal("7"), mergedSugarLine.userProvidedQuantity)
        assertEquals(BigDecimal("105.00"), mergedSugarLine.lineTotal)
        assertNotEquals(firstSugarLine.identity, mergedSugarLine.identity)
        assertTrue(result.problems.isEmpty())
    }

    @Test
    fun `lines in different units merge into the smallest selected unit and the total is recomputed`() {
        val dozenLine = saleLine(sugarId, "1", "180.00", unitId = dozenUnitId, storageUnitsPerUnit = 12)
        val pieceLine = saleLine(sugarId, "5", "75.00")

        val result = merge(listOf(dozenLine, pieceLine))

        val mergedLine = result.mergedSaleLines.single()
        assertEquals(BigDecimal("17"), mergedLine.userProvidedQuantity.stripTrailingZeros())
        assertEquals(pieceUnitId, mergedLine.unitId)
        assertEquals(1L, mergedLine.storageUnitsPerUserProvidedUnit)
        assertEquals(BigDecimal("255.00"), mergedLine.lineTotal)
    }

    @Test
    fun `lines of one unit id whose snapshotted sizes differ merge through the conversion, not a plain sum`() {
        val cartonUnitId = UUID.randomUUID()
        val olderCartonLine = saleLine(
            sugarId, "1", "24.00", unitId = cartonUnitId, storageUnitsPerUnit = 24, defaultSalePrice = "1.00"
        )
        val newerCartonLine = saleLine(
            sugarId, "1", "20.00", unitId = cartonUnitId, storageUnitsPerUnit = 20, defaultSalePrice = "1.00"
        )

        val result = merge(listOf(olderCartonLine, newerCartonLine))

        val mergedLine = result.mergedSaleLines.single()
        assertEquals(BigDecimal("2.2"), mergedLine.userProvidedQuantity)
        assertEquals(20L, mergedLine.storageUnitsPerUserProvidedUnit)
        assertEquals(BigDecimal("44.00"), mergedLine.lineTotal)
    }

    @Test
    fun `when the smallest unit cannot hold the quantity a larger selected unit is used`() {
        val litreUnitId = UUID.randomUUID()
        val bottleUnitId = UUID.randomUUID()
        val litreLine = saleLine(sugarId, "1", "15.00", unitId = litreUnitId, storageUnitsPerUnit = 1000)
        val bottleLine = saleLine(sugarId, "1", "11.25", unitId = bottleUnitId, storageUnitsPerUnit = 750)

        val result = merge(listOf(litreLine, bottleLine), storageUnitsPerBaseUnit = 1000)

        val mergedLine = result.mergedSaleLines.single()
        assertEquals(litreUnitId, mergedLine.unitId)
        assertEquals(BigDecimal("1.75"), mergedLine.userProvidedQuantity)
        assertEquals(BigDecimal("26.25"), mergedLine.lineTotal)
        assertTrue(result.problems.isEmpty())
    }

    @Test
    fun `typed prices that only work in a larger selected unit merge in that unit`() {
        val litreLine = saleLine(
            sugarId, "1", "20.00", unitId = UUID.randomUUID(), storageUnitsPerUnit = 1000, defaultSalePrice = "20.00"
        )
        val bottleLine = saleLine(
            sugarId, "1", "15.00", unitId = UUID.randomUUID(), storageUnitsPerUnit = 750, defaultSalePrice = "20.00"
        )

        val result = merge(
            listOf(litreLine, bottleLine),
            listOf(priceOverride(litreLine, "18.00"), priceOverride(bottleLine, "13.50")),
            storageUnitsPerBaseUnit = 1000,
        )

        val mergedLine = result.mergedSaleLines.single()
        assertEquals(litreLine.unitId, mergedLine.unitId)
        assertEquals(BigDecimal("1.75"), mergedLine.userProvidedQuantity)
        assertEquals(BigDecimal("18.00"), result.mergedSaleAdjustments.single().value)
        assertTrue(result.problems.isEmpty())
    }

    @Test
    fun `lines no selected unit can hold merge in a larger unit of the product's tree`() {
        val litreUnitId = UUID.randomUUID()
        val jerrycanLine = saleLine(
            sugarId, "1", "400.00", unitId = UUID.randomUUID(), storageUnitsPerUnit = 20000, defaultSalePrice = "20.00"
        )
        val bottleLine = saleLine(
            sugarId, "1", "15.00", unitId = UUID.randomUUID(), storageUnitsPerUnit = 750, defaultSalePrice = "20.00"
        )

        val result = merge(
            listOf(jerrycanLine, bottleLine), storageUnitsPerBaseUnit = 1000, familySizes = mapOf(litreUnitId to 1000L),
        )

        val mergedLine = result.mergedSaleLines.single()
        assertEquals(litreUnitId, mergedLine.unitId)
        assertEquals(BigDecimal("20.75"), mergedLine.userProvidedQuantity)
        assertEquals(BigDecimal("415.00"), mergedLine.lineTotal)
        assertTrue(result.problems.isEmpty())
    }

    @Test
    fun `typed prices no selected unit can hold merge in a larger unit of the tree`() {
        val litreUnitId = UUID.randomUUID()
        val jerrycanLine = saleLine(
            sugarId, "1", "400.00", unitId = UUID.randomUUID(), storageUnitsPerUnit = 20000, defaultSalePrice = "20.00"
        )
        val bottleLine = saleLine(
            sugarId, "1", "15.00", unitId = UUID.randomUUID(), storageUnitsPerUnit = 750, defaultSalePrice = "20.00"
        )

        val result = merge(
            listOf(jerrycanLine, bottleLine),
            listOf(priceOverride(jerrycanLine, "360.00"), priceOverride(bottleLine, "13.50")),
            storageUnitsPerBaseUnit = 1000,
            familySizes = mapOf(litreUnitId to 1000L),
        )

        val mergedLine = result.mergedSaleLines.single()
        assertEquals(litreUnitId, mergedLine.unitId)
        assertEquals(BigDecimal("18.00"), result.mergedSaleAdjustments.single().value)
        assertTrue(result.problems.isEmpty())
    }

    @Test
    fun `typed prices that fit cents per storage unit merge in the storage unit`() {
        val eightLine = saleLine(
            sugarId, "1", "8.00", unitId = UUID.randomUUID(), storageUnitsPerUnit = 8, defaultSalePrice = "1.00"
        )
        val nineLine = saleLine(
            sugarId, "1", "9.00", unitId = UUID.randomUUID(), storageUnitsPerUnit = 9, defaultSalePrice = "1.00"
        )

        val result = merge(
            listOf(eightLine, nineLine),
            listOf(priceOverride(eightLine, "0.80"), priceOverride(nineLine, "0.90")),
        )

        val mergedLine = result.mergedSaleLines.single()
        assertEquals(storageUnitId, mergedLine.unitId)
        assertEquals(BigDecimal("0.10"), result.mergedSaleAdjustments.single().value)
        assertTrue(result.problems.isEmpty())
    }

    @Test
    fun `typed prices that fit no unit of the tree are a problem and keep the lines`() {
        val eightLine = saleLine(
            sugarId, "1", "8.00", unitId = UUID.randomUUID(), storageUnitsPerUnit = 8, defaultSalePrice = "1.00"
        )
        val nineLine = saleLine(
            sugarId, "1", "9.00", unitId = UUID.randomUUID(), storageUnitsPerUnit = 9, defaultSalePrice = "1.00"
        )

        val result = merge(
            listOf(eightLine, nineLine),
            listOf(priceOverride(eightLine, "1.00"), priceOverride(nineLine, "1.00")),
        )

        assertEquals(listOf(ConsolidationProblemKind.TYPED_PRICE_NOT_TYPEABLE), result.problems.map { it.kind })
        assertEquals(setOf(eightLine, nineLine), result.problemSaleLines.toSet())
        assertTrue(result.mergedSaleLines.isEmpty())
    }

    @Test
    fun `lines in different units and list prices merge when every line has the same typed price per smallest unit`() {
        val dozenLine = saleLine(
            sugarId, "1", "180.00", unitId = dozenUnitId, storageUnitsPerUnit = 12, defaultSalePrice = "15.00"
        )
        val pieceLine = saleLine(sugarId, "5", "80.00", defaultSalePrice = "16.00")

        val result = merge(
            listOf(dozenLine, pieceLine),
            listOf(priceOverride(dozenLine, "144.00"), priceOverride(pieceLine, "12.00")),
        )

        val mergedLine = result.mergedSaleLines.single()
        assertEquals(pieceUnitId, mergedLine.unitId)
        assertEquals(BigDecimal("17"), mergedLine.userProvidedQuantity.stripTrailingZeros())
        assertEquals(BigDecimal("12.00"), result.mergedSaleAdjustments.single().value)
        assertTrue(result.problems.isEmpty())
    }

    @Test
    fun `lines of equal size under different unit ids merge as one unit through the conversion`() {
        val otherDozenUnitId = UUID.randomUUID()
        val firstDozenLine = saleLine(
            sugarId, "2", "360.00", unitId = dozenUnitId, storageUnitsPerUnit = 12, defaultSalePrice = "15.00"
        )
        val secondDozenLine = saleLine(
            sugarId, "3", "540.00", unitId = otherDozenUnitId, storageUnitsPerUnit = 12, defaultSalePrice = "15.00"
        )

        val result = merge(listOf(firstDozenLine, secondDozenLine))

        val mergedLine = result.mergedSaleLines.single()
        assertEquals(dozenUnitId, mergedLine.unitId)
        assertEquals(BigDecimal("5"), mergedLine.userProvidedQuantity.stripTrailingZeros())
        assertEquals(BigDecimal("900.00"), mergedLine.lineTotal)
    }

    @Test
    fun `an inactive product on several lines is a problem and every line is kept`() {
        val firstLine = saleLine(sugarId, "3", "45.00")
        val secondLine = saleLine(sugarId, "4", "60.00")

        val result = merge(listOf(firstLine, secondLine), inactiveLocationProductIds = setOf(sugarId))

        assertEquals(listOf(ConsolidationProblemKind.INACTIVE_PRODUCT), result.problems.map { it.kind })
        assertEquals(listOf(firstLine, secondLine), result.problemSaleLines)
        assertTrue(result.mergedSaleLines.isEmpty())
    }

    @Test
    fun `the reason reported is the one from the unit that got furthest`() {
        val litreUnitId = UUID.randomUUID()
        val jerrycanLine = saleLine(
            sugarId, "1", "400.00", unitId = UUID.randomUUID(), storageUnitsPerUnit = 20000, defaultSalePrice = "20.00"
        )
        val bottleLine = saleLine(
            sugarId, "1", "15.00", unitId = UUID.randomUUID(), storageUnitsPerUnit = 750, defaultSalePrice = "20.00"
        )

        val result = merge(
            listOf(jerrycanLine, bottleLine),
            listOf(priceOverride(jerrycanLine, "360.00"), priceOverride(bottleLine, "15.00")),
            storageUnitsPerBaseUnit = 1000,
            familySizes = mapOf(litreUnitId to 1000L),
        )

        assertEquals(listOf(ConsolidationProblemKind.TYPED_PRICES_DIFFER), result.problems.map { it.kind })
    }

    @Test
    fun `a quantity no selected unit can hold falls back to the storage unit`() {
        val eightUnitId = UUID.randomUUID()
        val nineUnitId = UUID.randomUUID()
        val eightLine = saleLine(
            sugarId, "1", "8.00", unitId = eightUnitId, storageUnitsPerUnit = 8, defaultSalePrice = "1.00"
        )
        val nineLine = saleLine(
            sugarId, "1", "9.00", unitId = nineUnitId, storageUnitsPerUnit = 9, defaultSalePrice = "1.00"
        )

        val result = merge(listOf(eightLine, nineLine))

        val mergedLine = result.mergedSaleLines.single()
        assertEquals(storageUnitId, mergedLine.unitId)
        assertEquals(BigDecimal("17"), mergedLine.userProvidedQuantity.stripTrailingZeros())
        assertTrue(result.problems.isEmpty())
    }

    @Test
    fun `a combined quantity over the maximum is a problem`() {
        val largeLine = saleLine(sugarId, "999999999999.99", "100.00")
        val extraLine = saleLine(sugarId, "1", "1.00")

        val result = merge(listOf(largeLine, extraLine))

        assertEquals(listOf(ConsolidationProblemKind.QUANTITY_TOO_LARGE), result.problems.map { it.kind })
    }

    @Test
    fun `typed prices that cannot be expressed per smallest unit in cents are a problem`() {
        val dozenLine = saleLine(
            sugarId, "1", "180.00", unitId = dozenUnitId, storageUnitsPerUnit = 12, defaultSalePrice = "15.00"
        )
        val pieceLine = saleLine(sugarId, "12", "180.00", defaultSalePrice = "15.00")

        val result = merge(
            listOf(dozenLine, pieceLine),
            listOf(priceOverride(dozenLine, "10.00"), priceOverride(pieceLine, "0.83")),
        )

        assertEquals(listOf(ConsolidationProblemKind.TYPED_PRICE_NOT_TYPEABLE), result.problems.map { it.kind })
    }

    @Test
    fun `lines priced from different list prices are a problem`() {
        val result = merge(
            listOf(saleLine(sugarId, "3", "45.00"), saleLine(sugarId, "3", "42.00", defaultSalePrice = "14.00"))
        )

        assertEquals(listOf(ConsolidationProblemKind.DIFFERENT_LIST_PRICES), result.problems.map { it.kind })
    }

    @Test
    fun `lines from different list prices merge when every line carries the same typed price`() {
        val firstLine = saleLine(sugarId, "5", "75.00", defaultSalePrice = "15.00")
        val secondLine = saleLine(sugarId, "5", "80.00", defaultSalePrice = "16.00")

        val result = merge(
            listOf(firstLine, secondLine),
            listOf(priceOverride(firstLine, "14.00"), priceOverride(secondLine, "14.00")),
        )

        val mergedLine = result.mergedSaleLines.single()
        assertEquals(BigDecimal("155.00"), mergedLine.lineTotal)
        assertEquals(BigDecimal("14.00"), result.mergedSaleAdjustments.single().value)
        assertTrue(result.problems.isEmpty())
    }

    @Test
    fun `lines from different list prices with typed prices that disagree are a problem`() {
        val firstLine = saleLine(sugarId, "5", "75.00", defaultSalePrice = "15.00")
        val secondLine = saleLine(sugarId, "5", "80.00", defaultSalePrice = "16.00")

        val result = merge(
            listOf(firstLine, secondLine),
            listOf(priceOverride(firstLine, "14.00"), priceOverride(secondLine, "13.00")),
        )

        assertEquals(listOf(ConsolidationProblemKind.TYPED_PRICES_DIFFER), result.problems.map { it.kind })
    }

    @Test
    fun `an inactive product is a problem even when it is on a single line`() {
        val result = merge(listOf(saleLine(sugarId, "3", "45.00")), inactiveLocationProductIds = setOf(sugarId))

        assertEquals(listOf(ConsolidationProblemKind.INACTIVE_PRODUCT), result.problems.map { it.kind })
        assertTrue(result.mergedSaleLines.isEmpty())
    }

    @Test
    fun `price overrides that agree per smallest unit merge into one override on the smallest unit`() {
        val pieceLine = saleLine(sugarId, "12", "180.00", defaultSalePrice = "15.00")
        val dozenLine = saleLine(
            sugarId, "1", "180.00", unitId = dozenUnitId, storageUnitsPerUnit = 12, defaultSalePrice = "15.00"
        )
        val pieceOverride = priceOverride(pieceLine, "12.00")
        val dozenOverride = priceOverride(dozenLine, "144.00")

        val result = merge(listOf(pieceLine, dozenLine), listOf(pieceOverride, dozenOverride))

        val mergedOverride = result.mergedSaleAdjustments.single()
        assertEquals(BigDecimal("12.00"), mergedOverride.value)
        assertEquals(AdjustmentDirection.DISCOUNT, mergedOverride.direction)
        assertEquals(result.mergedSaleLines.single().identity, mergedOverride.relatedSaleLineIdentity)
        assertTrue(result.problems.isEmpty())
    }

    @Test
    fun `lines where only some carry a price override are a problem`() {
        val overriddenLine = saleLine(sugarId, "3", "45.00")
        val plainLine = saleLine(sugarId, "3", "45.00")

        val result = merge(listOf(overriddenLine, plainLine), listOf(priceOverride(overriddenLine, "14.00")))

        assertEquals(listOf(ConsolidationProblemKind.OVERRIDE_ON_SOME_LINES_ONLY), result.problems.map { it.kind })
        assertEquals(1, result.problemSaleAdjustments.size)
    }

    @Test
    fun `a merged override that would shift the combined total by a cent is a drift problem`() {
        val firstLine = saleLine(sugarId, "1.50", "1.50", defaultSalePrice = "1.00")
        val secondLine = saleLine(sugarId, "1.50", "1.50", defaultSalePrice = "1.00")

        val result = merge(
            listOf(firstLine, secondLine),
            listOf(priceOverride(firstLine, "0.83"), priceOverride(secondLine, "0.83")),
        )

        assertEquals(listOf(ConsolidationProblemKind.TOTAL_DRIFT), result.problems.map { it.kind })
    }

    @Test
    fun `the same percentage discount on every line is carried once on the merged line`() {
        val firstLine = saleLine(sugarId, "1", "10.00", defaultSalePrice = "10.00")
        val secondLine = saleLine(sugarId, "1", "10.00", defaultSalePrice = "10.00")

        val result = merge(
            listOf(firstLine, secondLine),
            listOf(percentageDiscount(firstLine, "10"), percentageDiscount(secondLine, "10")),
        )

        val carriedDiscount = result.mergedSaleAdjustments.single()
        assertEquals(result.mergedSaleLines.single().identity, carriedDiscount.relatedSaleLineIdentity)
        assertTrue(result.problems.isEmpty())
    }

    @Test
    fun `the same percentage discount approved by different people is a problem`() {
        val firstLine = saleLine(sugarId, "1", "10.00", defaultSalePrice = "10.00")
        val secondLine = saleLine(sugarId, "1", "10.00", defaultSalePrice = "10.00")

        val result = merge(
            listOf(firstLine, secondLine),
            listOf(
                percentageDiscount(firstLine, "10", approvedById = UUID.randomUUID()),
                percentageDiscount(secondLine, "10", approvedById = UUID.randomUUID()),
            ),
        )

        assertEquals(listOf(ConsolidationProblemKind.APPROVERS_DIFFER), result.problems.map { it.kind })
    }

    @Test
    fun `the same typed price approved by different people is a problem`() {
        val firstLine = saleLine(sugarId, "5", "75.00")
        val secondLine = saleLine(sugarId, "5", "75.00")

        val result = merge(
            listOf(firstLine, secondLine),
            listOf(
                priceOverride(firstLine, "14.00", approvedById = UUID.randomUUID()),
                priceOverride(secondLine, "14.00", approvedById = UUID.randomUUID()),
            ),
        )

        assertEquals(listOf(ConsolidationProblemKind.APPROVERS_DIFFER), result.problems.map { it.kind })
    }

    @Test
    fun `different percentage discounts on the lines are a problem`() {
        val firstLine = saleLine(sugarId, "1", "10.00", defaultSalePrice = "10.00")
        val secondLine = saleLine(sugarId, "1", "10.00", defaultSalePrice = "10.00")

        val result = merge(
            listOf(firstLine, secondLine),
            listOf(percentageDiscount(firstLine, "10"), percentageDiscount(secondLine, "5")),
        )

        assertEquals(listOf(ConsolidationProblemKind.OTHER_ADJUSTMENTS_DIFFER), result.problems.map { it.kind })
    }

    @Test
    fun `order-level adjustments come back untouched and do not affect the merge`() {
        val firstLine = saleLine(sugarId, "3", "45.00")
        val secondLine = saleLine(sugarId, "4", "60.00")
        val orderLevelDiscount = fivePercentOrderLevelDiscount()

        val result = merge(listOf(firstLine, secondLine), listOf(orderLevelDiscount))

        assertEquals(listOf(orderLevelDiscount), result.orderLevelSaleAdjustments)
        assertEquals(BigDecimal("105.00"), result.mergedSaleLines.single().lineTotal)
        assertTrue(result.problems.isEmpty())
    }

    @Test
    fun `an adjustment on a line that is not in the sale is refused`() {
        val sugarLine = saleLine(sugarId, "3", "45.00")
        val adjustmentOnAnotherLine = percentageDiscount(saleLine(riceId, "2", "40.00"), "10")

        assertThrows(RtsGenericException::class.java) { merge(listOf(sugarLine), listOf(adjustmentOnAnotherLine)) }
    }

    @Test
    fun `a problem product keeps all its lines while clean products still merge`() {
        val firstSugarLine = saleLine(sugarId, "3", "45.00")
        val secondSugarLine = saleLine(sugarId, "3", "42.00", defaultSalePrice = "14.00")
        val firstRiceLine = saleLine(riceId, "2", "40.00")
        val secondRiceLine = saleLine(riceId, "2", "40.00")

        val result = merge(listOf(firstSugarLine, firstRiceLine, secondSugarLine, secondRiceLine))

        assertEquals(listOf(firstSugarLine, secondSugarLine), result.problemSaleLines)
        assertEquals(listOf(riceId), result.mergedSaleLines.map { it.locationProductId })
    }

    private fun merge(
        saleLines: List<SaleSessionLine>,
        saleAdjustments: List<SaleSessionAdjustment> = emptyList(),
        inactiveLocationProductIds: Set<UUID> = emptySet(),
        storageUnitsPerBaseUnit: Long = 1,
        familySizes: Map<UUID, Long> = emptyMap(),
    ): SaleLineMergeResult = SaleLineMerger.merge(
        saleLines = saleLines,
        saleAdjustments = saleAdjustments,
        inactiveLocationProductIds = inactiveLocationProductIds,
        unitConversionGraph = unitConversionGraph(storageUnitsPerBaseUnit, familySizes),
    )

    private fun unitConversionGraph(storageUnitsPerBaseUnit: Long, familySizes: Map<UUID, Long>) = UnitConversionGraph(
        fromUnitToTargets = emptyMap(),
        placementByUnitId = mapOf(
            pieceUnitId to UnitPlacement(storageUnitId, storageUnitsPerBaseUnit),
            storageUnitId to UnitPlacement(storageUnitId, 1),
        ),
        breakdownUnits = (familySizes + (storageUnitId to 1L)).map { (unitId, sizeInRoot) ->
            BreakdownUnit(unitId, "unit", sizeInRoot, storageUnitId, OffsetDateTime.now())
        },
    )

    private fun saleLine(
        locationProductId: UUID,
        userProvidedQuantity: String,
        lineTotal: String,
        unitId: UUID = pieceUnitId,
        storageUnitsPerUnit: Long = 1,
        defaultSalePrice: String = "15.00",
    ) = SaleSessionLine(
        identity = SessionIdentity.mintFreshIdentity(),
        locationProductId = locationProductId,
        productLabel = "Product",
        productPrintLabel = "Product",
        userProvidedQuantity = BigDecimal(userProvidedQuantity),
        unitId = unitId,
        baseUnitId = pieceUnitId,
        storageUnitsPerUserProvidedUnit = storageUnitsPerUnit,
        lineTotal = BigDecimal(lineTotal),
        defaultSalePrice = BigDecimal(defaultSalePrice),
    )

    private fun fivePercentOrderLevelDiscount() = SaleSessionAdjustment(
        identity = SessionIdentity.mintFreshIdentity(),
        relatedSaleLineIdentity = null,
        adjustmentReasonId = adjustmentReasonId,
        direction = AdjustmentDirection.DISCOUNT,
        calculationMethod = PriceAdjustmentMethod.PERCENTAGE,
        value = BigDecimal("5"),
        note = null,
        approvedById = null,
    )

    private fun priceOverride(
        saleLine: SaleSessionLine,
        typedUnitPrice: String,
        approvedById: UUID? = null,
    ): SaleSessionAdjustment {
        val typedPrice = BigDecimal(typedUnitPrice)
        val overrideDifference = saleLine.lineTotal.subtract(typedPrice.multiply(saleLine.userProvidedQuantity))
        val direction = if (overrideDifference.signum() >= 0) {
            AdjustmentDirection.DISCOUNT
        } else {
            AdjustmentDirection.SURCHARGE
        }
        return adjustment(saleLine, direction, PriceAdjustmentMethod.UNIT_PRICE_OVERRIDE, typedPrice, approvedById)
    }

    private fun percentageDiscount(saleLine: SaleSessionLine, percentage: String, approvedById: UUID? = null) =
        adjustment(
            saleLine,
            AdjustmentDirection.DISCOUNT,
            PriceAdjustmentMethod.PERCENTAGE,
            BigDecimal(percentage),
            approvedById,
        )

    private fun adjustment(
        saleLine: SaleSessionLine,
        direction: AdjustmentDirection,
        calculationMethod: PriceAdjustmentMethod,
        value: BigDecimal,
        approvedById: UUID?,
    ) = SaleSessionAdjustment(
        identity = SessionIdentity.mintFreshIdentity(),
        relatedSaleLineIdentity = saleLine.identity,
        adjustmentReasonId = adjustmentReasonId,
        direction = direction,
        calculationMethod = calculationMethod,
        value = value,
        note = null,
        approvedById = approvedById,
    )
}
