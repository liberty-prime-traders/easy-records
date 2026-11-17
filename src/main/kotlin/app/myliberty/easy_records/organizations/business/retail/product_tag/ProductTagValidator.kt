package app.myliberty.easy_records.organizations.business.retail.product_tag

import app.myliberty.easy_records.organizations.business.core.tag.api.TagService
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProductTagValidator(
    private val tagService: TagService
) {

    fun validateTagsExist(tagIds: Set<UUID>) {
        if (tagIds.isEmpty()) return

        val existingTagIds = tagService.getAllTagDtos().map { it.id }.toSet()
        val missingTagIds = tagIds - existingTagIds
        if (missingTagIds.isNotEmpty()) {
            throw LibertyGenericException(
                "The following tag IDs do not exist: ${missingTagIds.joinToString(", ")}"
            )
        }
    }

    fun validateNoOverlap(tagsToAdd: Set<UUID>, tagsToRemove: Set<UUID>) {
        val overlap = tagsToAdd.intersect(tagsToRemove)
        if (overlap.isNotEmpty()) {
            throw LibertyGenericException(
                "The following tag IDs are present in both tagsToAdd and tagsToRemove: ${overlap.joinToString(", ")}"
            )
        }
    }
}
