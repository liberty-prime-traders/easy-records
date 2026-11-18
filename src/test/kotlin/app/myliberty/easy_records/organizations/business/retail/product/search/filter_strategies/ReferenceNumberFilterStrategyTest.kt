package app.myliberty.easy_records.organizations.business.retail.product.search.filter_strategies

import app.myliberty.easy_records.cross_tier.retail.product.search.common.filters.ReferenceNumberFilterStrategy
import app.myliberty.easy_records.util.queries.QueryBuilderContext
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReferenceNumberFilterStrategyTest {

  @Test
  fun `applies LIKE filter with trailing percent`() {
    val context = QueryBuilderContext()
    ReferenceNumberFilterStrategy("REF123").apply(context)

    assertTrue(context.whereClauses[0].contains("p.reference_number LIKE :referenceNumber"))
    assertEquals("REF123%", context.params["referenceNumber"])
  }

  @Test
  fun `null reference number ignored`() {
    val context = QueryBuilderContext()
    ReferenceNumberFilterStrategy(null).apply(context)

    assertTrue(context.whereClauses.isEmpty())
  }

  @Test
  fun `blank reference number ignored`() {
    val context = QueryBuilderContext()
    ReferenceNumberFilterStrategy("   ").apply(context)

    assertTrue(context.whereClauses.isEmpty())
  }

  @Test
  fun `preserves special characters`() {
    val context = QueryBuilderContext()
    ReferenceNumberFilterStrategy("REF-123_ABC").apply(context)

    assertEquals("REF-123_ABC%", context.params["referenceNumber"])
  }
}
