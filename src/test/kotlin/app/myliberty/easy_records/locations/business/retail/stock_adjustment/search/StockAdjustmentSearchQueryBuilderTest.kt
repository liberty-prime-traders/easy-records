package app.myliberty.easy_records.locations.business.retail.stock_adjustment.search

import app.myliberty.easy_records.locations.business.retail.stock.api.StockAdjustmentCategory
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.api.StockAdjustmentSearchParameters
import app.myliberty.easy_records.util.queries.KeysetSearchCursor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class StockAdjustmentSearchQueryBuilderTest {

    private val adjustedFrom = OffsetDateTime.parse("2026-01-01T00:00:00Z")
    private val adjustedBefore = OffsetDateTime.parse("2026-02-01T00:00:00Z")

    @Test
    fun `adjusted range is inclusive of from and exclusive of before on created_on`() {
        val predicate = StockAdjustmentSearchQueryBuilder.buildPredicate(
            StockAdjustmentSearchParameters(adjustedFrom = adjustedFrom, adjustedBefore = adjustedBefore)
        )

        val whereClauses = predicate.whereClauses
        assertTrue(whereClauses.contains("sa.created_on >= :adjustedFrom"), whereClauses.toString())
        assertTrue(whereClauses.contains("sa.created_on < :adjustedBefore"), whereClauses.toString())
    }

    @Test
    fun `empty lists add no filter`() {
        val predicate = StockAdjustmentSearchQueryBuilder.buildPredicate(StockAdjustmentSearchParameters())

        assertEquals(listOf("1=1"), predicate.whereClauses)
    }

    @Test
    fun `lists filter on product, category code and creator`() {
        val locationProductId = UUID.randomUUID()
        val adjustedById = UUID.randomUUID()

        val predicate = StockAdjustmentSearchQueryBuilder.buildPredicate(
            StockAdjustmentSearchParameters(
                locationProductIds = listOf(locationProductId),
                categories = listOf(StockAdjustmentCategory.FREE_GOODS),
                adjustedByIds = listOf(adjustedById)
            )
        )

        assertTrue(predicate.whereClauses.contains("sa.location_product_id = ANY(:locationProductIds)"))
        assertTrue(predicate.whereClauses.contains("sa.category = ANY(:categories)"))
        assertTrue(predicate.whereClauses.contains("sa.created_by_id = ANY(:adjustedByIds)"))
        assertEquals(listOf("FG"), (predicate.params.getValue("categories") as Array<*>).toList())
    }

    @Test
    fun `the list is newest first and keysets with a less-than cursor`() {
        val cursor = KeysetSearchCursor(adjustedFrom, UUID.randomUUID())

        val predicate = StockAdjustmentSearchQueryBuilder.buildPredicate(StockAdjustmentSearchParameters())
        val sql = StockAdjustmentSearchQueryBuilder.buildListQuery(predicate, cursor).sql

        assertTrue(sql.contains("created_on DESC") && sql.contains("sa.id DESC"), sql)
        assertTrue(sql.contains(") < ("), sql)
    }
}
