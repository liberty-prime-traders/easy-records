package app.myliberty.easy_records.locations.business.sale_session

import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionLine
import java.math.BigDecimal

object SaleLineRanker {

    private val productLabelOrder: Comparator<String> = String.CASE_INSENSITIVE_ORDER
    private val fractionPerSameProductLine = BigDecimal("0.01")

    fun alphabetical(saleLines: List<SaleSessionLine>): List<SaleSessionLine> =
        saleLines
            .sortedWith(compareBy(productLabelOrder) { it.productLabel })
            .mapIndexed { lineIndex, saleLine -> saleLine.copy(rank = BigDecimal(lineIndex + 1)) }

    // Hundredths leave room for 99 lines of one product before they would spill into the next rank.
    fun alphabeticalWithLikeProductsTogether(saleLines: List<SaleSessionLine>): List<SaleSessionLine> {
        val saleLinesPerProduct = saleLines
            .groupBy { it.locationProductId }.values
            .sortedWith(compareBy(productLabelOrder) { productLines -> productLines.first().productLabel })
        return saleLinesPerProduct.flatMapIndexed { productIndex, productLines ->
            productLines.mapIndexed { lineIndex, saleLine ->
                saleLine.copy(
                    rank = BigDecimal(productIndex + 1) + fractionPerSameProductLine * BigDecimal(lineIndex)
                )
            }
        }
    }

    // Ascending, so the first added line ends up on top of the others added with it.
    fun ranksAboveAll(existingSaleLines: List<SaleSessionLine>, count: Int): List<BigDecimal> {
        val lowestRank = existingSaleLines.minOfOrNull { it.rank } ?: BigDecimal.ONE
        return (count downTo 1).map { lowestRank - BigDecimal(it) }
    }
}
