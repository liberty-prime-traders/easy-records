package app.myliberty.easy_records.organizations.business.organization_user

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.cross_tier.authority.MembershipSummaryRow
import app.myliberty.easy_records.organizations.business.organization_user.api.OrganizationUserInsertDto
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.ORGANIZATION_USER])
class OrganizationUserCache(
    private val organizationUserRepository: OrganizationUserRepository,
    private val organizationUserMapper: OrganizationUserMapper
) {
    @Cacheable
    fun getOrganizationUsers(): Collection<OrganizationUserDto> {
        return organizationUserRepository.findAll().map { organizationUserMapper.toDomainDto(it) }
    }

    @Cacheable
    fun getMembershipSummaries(): List<MembershipSummaryRow> {
        return organizationUserRepository.findMembershipSummaries()
            .map { MembershipSummaryRow(it.getUserId(), it.getMembershipCount(), it.getActive()) }
    }

    @CacheEvict(allEntries = true)
    fun create(insertDto: OrganizationUserInsertDto): OrganizationUserDto {
        val saved = organizationUserRepository.save(organizationUserMapper.toEntity(insertDto))
        return organizationUserMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun createAll(insertDtos: Collection<OrganizationUserInsertDto>): List<OrganizationUserDto> {
        val entities = insertDtos.map { organizationUserMapper.toEntity(it) }
        return organizationUserRepository.saveAllAndFlush(entities).map { organizationUserMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun save(organizationUserDto: OrganizationUserDto): OrganizationUserDto {
        val saved = organizationUserRepository.save(organizationUserMapper.toEntity(organizationUserDto))
        return organizationUserMapper.toDomainDto(saved)
    }
}
