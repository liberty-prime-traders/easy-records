package app.myliberty.easy_records.locations.business.sale_session

import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.sale_session.api.SessionIdentity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class SaleLineRankerTest {

    private val riceId = UUID.randomUUID()
    private val sugarId = UUID.randomUUID()

    @Test
    fun `lines are ranked alphabetically by product, ignoring case`() {
        val rankedLines = SaleLineRanker.alphabetical(listOf(line(sugarId, "sugar"), line(riceId, "Rice")))

        assertEquals(
            listOf("Rice" to BigDecimal(1), "sugar" to BigDecimal(2)),
            rankedLines.map { it.productLabel to it.rank }
        )
    }

    @Test
    fun `like products share a whole rank and take hundredths in the order the lines came`() {
        val firstRice = line(riceId, "Rice", "1")
        val sugar = line(sugarId, "Sugar", "2")
        val secondRice = line(riceId, "Rice", "3")

        val rankedLines = SaleLineRanker.alphabeticalWithLikeProductsTogether(listOf(firstRice, sugar, secondRice))

        assertEquals(
            listOf(
                Triple("Rice", "1", BigDecimal("1.00")),
                Triple("Rice", "3", BigDecimal("1.01")),
                Triple("Sugar", "2", BigDecimal("2.00")),
            ),
            rankedLines.map { Triple(it.productLabel, it.userProvidedQuantity.toPlainString(), it.rank) }
        )
    }

    @Test
    fun `added lines rank above every existing line and keep the order they were added in`() {
        val existingLines = listOf(line(riceId, "Rice", rank = "1"), line(sugarId, "Sugar", rank = "2"))

        val ranks = SaleLineRanker.ranksAboveAll(existingLines, 2)

        assertEquals(listOf(BigDecimal(-1), BigDecimal(0)), ranks)
    }

    @Test
    fun `added lines on an empty session are still ordered`() {
        assertEquals(listOf(BigDecimal(0)), SaleLineRanker.ranksAboveAll(emptyList(), 1))
    }

    private fun line(locationProductId: UUID, productLabel: String, quantity: String = "1", rank: String = "0") =
        SaleSessionLine(
            identity = SessionIdentity.mintFreshIdentity(),
            locationProductId = locationProductId,
            productLabel = productLabel,
            productPrintLabel = productLabel,
            userProvidedQuantity = BigDecimal(quantity),
            unitId = UUID.randomUUID(),
            baseUnitId = UUID.randomUUID(),
            storageUnitsPerUserProvidedUnit = 1L,
            lineTotal = BigDecimal("10.00"),
            defaultSalePrice = BigDecimal("10.00"),
            rank = BigDecimal(rank),
        )
}
