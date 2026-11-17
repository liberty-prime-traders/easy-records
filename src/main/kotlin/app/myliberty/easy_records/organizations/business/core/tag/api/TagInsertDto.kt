package app.myliberty.easy_records.organizations.business.tag.api

import java.io.Serializable

data class TagInsertDto(
    val category: CategoryType,
    val tagName: String,
    val description: String? = null,
) : Serializable
