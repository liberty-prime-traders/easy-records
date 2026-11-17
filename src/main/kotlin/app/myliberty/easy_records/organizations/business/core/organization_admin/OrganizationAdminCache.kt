package app.myliberty.easy_records.organizations.business.core.organization_admin

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.organizations.business.core.organization_admin.api.OrganizationAdminInsertDto
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.ORGANIZATION_ADMIN])
class OrganizationAdminCache(
    private val organizationAdminRepository: OrganizationAdminRepository,
    private val organizationAdminMapper: OrganizationAdminMapper
) {

    @Cacheable
    fun getAdminHistory(): Collection<OrganizationAdminDto> {
        return organizationAdminRepository.findAll().map { organizationAdminMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun create(insertDto: OrganizationAdminInsertDto): OrganizationAdminDto {
        val saved = organizationAdminRepository.save(organizationAdminMapper.toEntity(insertDto))
        return organizationAdminMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(dto: OrganizationAdminDto): OrganizationAdminDto {
        val saved = organizationAdminRepository.save(organizationAdminMapper.toEntity(dto))
        return organizationAdminMapper.toDomainDto(saved)
    }
}
