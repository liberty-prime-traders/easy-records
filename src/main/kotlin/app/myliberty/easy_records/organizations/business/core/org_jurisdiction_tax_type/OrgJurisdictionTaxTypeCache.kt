package app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeDto
import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeInsertDto
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.ORG_JURISDICTION_TAX_TYPE])
class OrgJurisdictionTaxTypeCache(
    private val orgJurisdictionTaxTypeRepository: OrgJurisdictionTaxTypeRepository,
    private val orgJurisdictionTaxTypeMapper: OrgJurisdictionTaxTypeMapper
) {

    @Cacheable
    fun getAll(): Collection<OrgJurisdictionTaxTypeDto> =
        orgJurisdictionTaxTypeRepository.findAll().map { orgJurisdictionTaxTypeMapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun createAll(insertDtos: Collection<OrgJurisdictionTaxTypeInsertDto>): List<OrgJurisdictionTaxTypeDto> {
        val entities = insertDtos.map { orgJurisdictionTaxTypeMapper.toEntity(it) }
        return orgJurisdictionTaxTypeRepository.saveAllAndFlush(entities).map { orgJurisdictionTaxTypeMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun saveAll(dtos: Collection<OrgJurisdictionTaxTypeDto>) {
        orgJurisdictionTaxTypeRepository.saveAll(dtos.map { orgJurisdictionTaxTypeMapper.toEntity(it) })
    }
}
