package app.myliberty.easy_records.cross_tier.retail.product.search.common

import app.myliberty.easy_records.util.exceptions.LibertyGenericException

object ProductSearchValidator {

  private const val MAX_ARRAY_SIZE = 50

  fun validateArraySizes(
    categoryIds: Collection<*>,
    statusList: Collection<*>,
    tagIds: Collection<*>? = null
  ) {
    if (categoryIds.size > MAX_ARRAY_SIZE) {
      throw LibertyGenericException("categoryIds exceeds maximum size of $MAX_ARRAY_SIZE")
    }
    if (statusList.size > MAX_ARRAY_SIZE) {
      throw LibertyGenericException("statusList exceeds maximum size of $MAX_ARRAY_SIZE")
    }
    tagIds?.let {
      if (it.size > MAX_ARRAY_SIZE) {
        throw LibertyGenericException("tagIds exceeds maximum size of $MAX_ARRAY_SIZE")
      }
    }
  }
}
