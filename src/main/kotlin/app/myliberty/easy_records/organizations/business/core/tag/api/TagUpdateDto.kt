package app.myliberty.easy_records.organizations.business.core.tag.api

import app.myliberty.easy_records.util.business.StringUtils
import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class TagUpdateDto(
    val id: UUID? = null,
    val category: Optional<CategoryType>? = null,
    val tagName: Optional<String>? = null,
    val description: Optional<String>? = null,
) : Serializable {

    fun applyTo(existing: TagDto): TagDto = existing.copy(
        category = category?.orElse(existing.category) ?: existing.category,
        tagName = StringUtils.useIfProvided(tagName, existing.tagName) ?: existing.tagName,
        description = description?.orElse(existing.description) ?: existing.description
    )
}
