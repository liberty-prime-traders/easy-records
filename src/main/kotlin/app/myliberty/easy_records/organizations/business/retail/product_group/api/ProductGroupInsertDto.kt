package app.myliberty.easy_records.organizations.business.retail.product_group.api

import java.io.Serializable
import java.util.UUID

data class ProductGroupInsertDto(
    val groupName: String,
    val description: String? = null,
    val categoryId: UUID
) : Serializable
