package app.myliberty.easy_records.organizations.business.retail.product.search

import app.myliberty.easy_records.cross_tier.retail.product.search.common.ProductSearchParameters
import app.myliberty.easy_records.cross_tier.retail.product.search.common.ProductSearchValidator
import app.myliberty.easy_records.organizations.business.retail.product.api.ProductStatus
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class ProductSearchValidatorTest {

  @Test
  fun `accepts arrays at max size of 50`() {
    val params = ProductSearchParameters(
      categoryIds = (1..50).map { UUID.randomUUID() }.toSet(),
      tagIds = (1..50).map { UUID.randomUUID() }.toSet(),
      statusList = setOf(ProductStatus.ACTIVE, ProductStatus.DISCONTINUED, ProductStatus.AWAITING_FINAL_SALE)
    )

    assertDoesNotThrow {
      ProductSearchValidator.validateArraySizes(params.categoryIds, params.statusList, params.tagIds)
    }
  }

  @Test
  fun `rejects categoryIds exceeding 50`() {
    val params = ProductSearchParameters(
      categoryIds = (1..51).map { UUID.randomUUID() }.toSet()
    )

    val exception = assertThrows<LibertyGenericException> {
      ProductSearchValidator.validateArraySizes(params.categoryIds, params.statusList, params.tagIds)
    }
    assertEquals(exception.message.contains("categoryIds exceeds maximum size of 50"), true)
  }

  @Test
  fun `rejects tagIds exceeding 50`() {
    val params = ProductSearchParameters(
      tagIds = (1..51).map { UUID.randomUUID() }.toSet()
    )

    val exception = assertThrows<LibertyGenericException> {
      ProductSearchValidator.validateArraySizes(params.categoryIds, params.statusList, params.tagIds)
    }
    assertEquals(exception.message.contains("tagIds exceeds maximum size of 50"), true)
  }
}
