package app.myliberty.easy_records.organizations.business.retail.product_category.api

import app.myliberty.easy_records.util.business.StringUtils
import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class ProductCategoryUpdateDto(
    val id: UUID? = null,
    val categoryName: Optional<String>? = null,
    val description: Optional<String>? = null,
) : Serializable {

    fun applyTo(existing: ProductCategoryDto): ProductCategoryDto = existing.copy(
        categoryName = categoryName?.orElse(existing.categoryName) ?: existing.categoryName,
        description = StringUtils.useIfProvided(description, existing.description)
    )
}
