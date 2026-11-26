package app.myliberty.easy_records.organizations.business.core.tag

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.organizations.business.core.tag.api.TagDto
import app.myliberty.easy_records.organizations.business.core.tag.api.TagInsertDto
import app.myliberty.easy_records.organizations.business.core.tag.mapping.TagMapper
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.TAG])
class TagCache(
    private val tagRepository: TagRepository,
    private val tagMapper: TagMapper
) {

    @Cacheable
    fun getAllTags(): Collection<TagDto> = tagRepository.findAll().map { tagMapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun create(insertDto: TagInsertDto): TagDto {
        val saved = tagRepository.save(tagMapper.toEntity(insertDto))
        return tagMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(tagDto: TagDto): TagDto {
        val saved = tagRepository.save(tagMapper.toEntity(tagDto))
        return tagMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun deleteTag(id: UUID) {
        tagRepository.deleteById(id)
    }
}
