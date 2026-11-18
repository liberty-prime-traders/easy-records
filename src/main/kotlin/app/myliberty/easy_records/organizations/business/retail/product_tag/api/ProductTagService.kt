package app.myliberty.easy_records.organizations.business.retail.product_tag.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.retail.product.api.OrganizationProductResponseDto
import app.myliberty.easy_records.organizations.business.retail.product.api.TagSummaryDto
import app.myliberty.easy_records.organizations.business.retail.product_tag.ProductTagCache
import app.myliberty.easy_records.organizations.business.retail.product_tag.ProductTagInsertDto
import app.myliberty.easy_records.organizations.business.retail.product_tag.ProductTagValidator
import app.myliberty.easy_records.organizations.business.core.tag.api.TagService
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class ProductTagService(
    private val productTagCache: ProductTagCache,
    private val productTagValidator: ProductTagValidator,
    private val tagService: TagService
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun findActiveTagIdsByOrgProductId(orgProductId: UUID): Collection<UUID> =
        productTagCache.findActiveTagIdsByOrgProductId(orgProductId)

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun populateTagsForProducts(products: List<OrganizationProductResponseDto>): List<OrganizationProductResponseDto> {
        if (products.isEmpty()) return products
        val orgProductIds = products.map { it.id }
        val productTags = productTagCache.findActiveProductTagsByOrgProductIds(orgProductIds)
        val tagIdsByOrgProductId = productTags.groupBy({ it.orgProductId }, { it.tagId })
        val tagsById = tagService.getAllTagDtos().associateBy { it.id }
        return products.map { product ->
            val tagIds = tagIdsByOrgProductId[product.id] ?: emptyList()
            val tags = tagIds.mapNotNull { tagId ->
                tagsById[tagId]?.let { tag -> TagSummaryDto(id = tag.id, tagName = tag.tagName) }
            }
            product.copy(activeTags = tags)
        }
    }

    fun manageProductTags(
        orgProductId: UUID,
        tagsToAdd: Set<UUID> = emptySet(),
        tagsToRemove: Set<UUID> = emptySet()
    ) {
        val combinedTags = tagsToAdd + tagsToRemove
        productTagValidator.validateTagsExist(combinedTags)
        productTagValidator.validateNoOverlap(tagsToAdd, tagsToRemove)

        val activeProductTags = productTagCache.findActiveProductTagsByOrgProductId(orgProductId)
        val activeTagIds = activeProductTags.map { it.tagId }

        val dtosToUpdate = tagsToRemove.mapNotNull { tagId ->
            activeProductTags.find { it.tagId == tagId }?.copy(endOn = OffsetDateTime.now())
        }

        if (dtosToUpdate.isNotEmpty()) {
            productTagCache.saveAllProductTags(dtosToUpdate)
        }

        val insertDtos = tagsToAdd
            .filter { !activeTagIds.contains(it) }
            .map { tagId -> ProductTagInsertDto(orgProductId = orgProductId, tagId = tagId) }

        if (insertDtos.isNotEmpty()) {
            productTagCache.createProductTags(insertDtos)
        }
    }
}
