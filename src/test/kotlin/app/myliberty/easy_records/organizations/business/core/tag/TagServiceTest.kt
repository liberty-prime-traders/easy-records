package app.myliberty.easy_records.organizations.business.core.tag

import app.myliberty.easy_records.organizations.business.core.tag.api.CategoryType
import app.myliberty.easy_records.organizations.business.core.tag.api.TagDto
import app.myliberty.easy_records.organizations.business.core.tag.api.TagService
import app.myliberty.easy_records.organizations.business.core.tag.api.TagUpdateDto
import app.myliberty.easy_records.organizations.business.core.tag.mapping.TagMapper
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class TagServiceTest {

    private val tagCache: TagCache = mock(TagCache::class.java)
    private val tagRepository: TagRepository = mock(TagRepository::class.java)
    private val tagMapper: TagMapper = mock(TagMapper::class.java)
    private val tagService = TagService(tagMapper, tagCache, tagRepository)

    private val existingTagId: UUID = UUID.fromString("0f1e2d3c-4b5a-4968-8777-665544332211")
    private val existingTag = TagDto(
        id = existingTagId,
        createdById = UUID.fromString("11111111-2222-3333-4444-555555555555"),
        createdOn = OffsetDateTime.parse("2026-01-01T00:00:00Z"),
        referenceNumber = "TG000001",
        category = CategoryType.PRODUCT,
        tagName = "Organic",
        description = "Certified organic",
    )

    // Kotlin rejects the null that argThat returns, so the matcher is registered and a placeholder is returned
    private fun savedTagDtoMatching(expectedTagName: String, expectedDescription: String): TagDto {
        argThat<TagDto> { savedDto ->
            val nameMatches = savedDto.tagName == expectedTagName
            val categoryMatches = savedDto.category == CategoryType.PRODUCT
            val descriptionMatches = savedDto.description == expectedDescription
            nameMatches && categoryMatches && descriptionMatches
        }
        return existingTag
    }

    @Test
    fun `updating a tag keeps the fields the update does not carry`() {
        val updateDto = TagUpdateDto(id = existingTagId, tagName = Optional.of("Natural"))
        `when`(tagCache.getAllTags()).thenReturn(listOf(existingTag))
        `when`(tagRepository.findSimilarTags("Natural", TagService.SIMILARITY_THRESHOLD)).thenReturn(emptyList())

        tagService.updateTag(updateDto)

        verify(tagCache).save(savedTagDtoMatching("Natural", "Certified organic"))
    }

    @Test
    fun `updating a tag to a new case of its own name is not a duplicate of itself`() {
        val updateDto = TagUpdateDto(id = existingTagId, tagName = Optional.of("ORGANIC"))
        `when`(tagCache.getAllTags()).thenReturn(listOf(existingTag))
        `when`(tagRepository.findSimilarTags("ORGANIC", TagService.SIMILARITY_THRESHOLD)).thenReturn(emptyList())

        tagService.updateTag(updateDto)

        verify(tagCache).save(savedTagDtoMatching("ORGANIC", "Certified organic"))
    }

    @Test
    fun `deleting an existing tag evicts it through the cache`() {
        `when`(tagCache.getAllTags()).thenReturn(listOf(existingTag))

        tagService.deleteTag(existingTagId)

        verify(tagCache).deleteTag(existingTagId)
    }

    @Test
    fun `deleting an unknown tag never reaches the cache delete`() {
        val unknownTagId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        `when`(tagCache.getAllTags()).thenReturn(listOf(existingTag))

        tagService.deleteTag(unknownTagId)

        verify(tagCache, never()).deleteTag(unknownTagId)
    }

    @Test
    fun `an explicit null tag name is rejected instead of keeping the stored name`() {
        val updateDto = TagUpdateDto(id = existingTagId, tagName = Optional.empty())

        val exception = assertThrows(LibertyGenericException::class.java) { tagService.updateTag(updateDto) }

        assertEquals(TagService.NAME_IS_REQUIRED, exception.message)
        verifyNoInteractions(tagCache)
    }

    @Test
    fun `deleting a tag still in use is rejected as a bad request`() {
        `when`(tagCache.getAllTags()).thenReturn(listOf(existingTag))
        doThrow(DataIntegrityViolationException("tag is referenced")).`when`(tagCache).deleteTag(existingTagId)

        val exception = assertThrows(LibertyGenericException::class.java) { tagService.deleteTag(existingTagId) }

        assertEquals(TagService.TAG_IN_USE, exception.message)
        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
    }
}
