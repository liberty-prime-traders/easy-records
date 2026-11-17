package app.myliberty.easy_records.organizations.business.retail.product_category.api

import java.io.Serializable

data class ProductCategoryInsertDto(
    val categoryName: String? = null,
    val description: String? = null,
) : Serializable
