package app.myliberty.easy_records.organizations.business.jobtitle

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.organizations.business.jobtitle.api.JobTitleInsertDto
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.TITLE])
class JobTitleCache(
    private val jobTitleRepository: JobTitleRepository,
    private val jobTitleMapper: JobTitleMapper
) {

    @Cacheable
    fun getAllJobTitles(): Collection<JobTitleDto> = jobTitleRepository.findAll().map { jobTitleMapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun create(insertDto: JobTitleInsertDto): JobTitleDto {
        val saved = jobTitleRepository.save(jobTitleMapper.toEntity(insertDto))
        return jobTitleMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(jobTitleDto: JobTitleDto): JobTitleDto {
        val saved = jobTitleRepository.save(jobTitleMapper.toEntity(jobTitleDto))
        return jobTitleMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun deleteJobTitle(id: UUID) {
        jobTitleRepository.deleteById(id)
    }
}
