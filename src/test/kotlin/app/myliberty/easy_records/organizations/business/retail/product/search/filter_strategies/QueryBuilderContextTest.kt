package app.myliberty.easy_records.organizations.business.retail.product.search.filter_strategies

import app.myliberty.easy_records.cross_tier.retail.product.search.common.filters.NameFilterStrategy
import app.myliberty.easy_records.cross_tier.retail.product.search.common.filters.StatusFilterStrategy
import app.myliberty.easy_records.cross_tier.retail.product.search.common.filters.TextSearchFilterStrategy
import app.myliberty.easy_records.organizations.business.retail.product.TagFilterStrategy
import app.myliberty.easy_records.util.queries.QueryBuilderContext
import app.myliberty.easy_records.util.queries.SearchStrategy
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueryBuilderContextTest {

  @Test
  fun `accumulates filters from multiple strategies`() {
    val context = QueryBuilderContext()

    NameFilterStrategy("Product A").apply(context)
    StatusFilterStrategy(setOf("A")).apply(context)
    TextSearchFilterStrategy("laptop", SearchStrategy.FULLTEXT).apply(context)

    assertEquals(3, context.whereClauses.size)
    assertEquals(3, context.params.size)
  }

  @Test
  fun `supports both WHERE and HAVING clauses`() {
    val context = QueryBuilderContext()

    NameFilterStrategy("").apply(context)
    TagFilterStrategy(setOf(UUID.randomUUID())).apply(context)

    assertTrue(context.whereClauses.isNotEmpty())
    assertTrue(context.havingClauses.isNotEmpty())
  }

  @Test
  fun `initializes with empty collections`() {
    val context = QueryBuilderContext()

    assertTrue(context.whereClauses.isEmpty())
    assertTrue(context.havingClauses.isEmpty())
    assertTrue(context.params.isEmpty())
  }
}
