package app.myliberty.easy_records.organizations.business.retail.product_group.api

import app.myliberty.easy_records.util.business.StringUtils
import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class ProductGroupUpdateDto(
    val id: UUID,
    val groupName: Optional<String>? = null,
    val description: Optional<String>? = null,
    val categoryId: Optional<UUID>? = null
) : Serializable {

    fun applyTo(existing: ProductGroupDto): ProductGroupDto = existing.copy(
        groupName = groupName?.orElse(existing.groupName) ?: existing.groupName,
        description = StringUtils.useIfProvided(description, existing.description),
        categoryId = categoryId?.orElse(existing.categoryId) ?: existing.categoryId
    )
}
