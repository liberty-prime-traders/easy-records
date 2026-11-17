package app.myliberty.easy_records.organizations.business.product

import app.myliberty.easy_records.organizations.business.product.api.TagSummaryDto
import app.myliberty.easy_records.organizations.business.product_tag.api.ProductTagService
import app.myliberty.easy_records.organizations.business.product_tag.api.ActiveProductTags
import app.myliberty.easy_records.organizations.business.tag.api.TagService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProductTagQualifier(
    private val productTagService: ProductTagService,
    private val tagService: TagService
) {

    @ActiveProductTags
    fun mapOrgProductIdToActiveTags(orgProductId: UUID?): List<TagSummaryDto> {
        if (orgProductId == null) return emptyList()
        val activeTagIds = productTagService.findActiveTagIdsByOrgProductId(orgProductId)
        if (activeTagIds.isEmpty()) return emptyList()
        return tagService.getAllTagDtos()
            .filter { activeTagIds.contains(it.id) }
            .map { tag -> TagSummaryDto(id = tag.id, tagName = tag.tagName) }
    }
}
