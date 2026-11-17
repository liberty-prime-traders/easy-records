package app.myliberty.easy_records.organizations.business.org_table_registry

import app.myliberty.easy_records.organizations.business.org_table_registry.api.OrgTableRegistryDto
import app.myliberty.easy_records.organizations.business.org_table_registry.api.OrgTableRegistryResponseDto
import app.myliberty.easy_records.organizations.business.org_table_registry.mapping.TableNameQualifier
import app.myliberty.easy_records.organizations.business.org_table_registry.mapping.TableNameResolver
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(
    config = RtsMapperConfig::class,
    uses = [TableNameResolver::class]
)
interface OrgTableRegistryMapper {

    fun toDomainDto(orgTableRegistryEntity: OrgTableRegistryEntity): OrgTableRegistryDto

    fun toEntity(dto: OrgTableRegistryDto): OrgTableRegistryEntity

    @Mapping(source = "registryId", target = "tableName", qualifiedBy = [TableNameQualifier::class])
    fun toDto(dto: OrgTableRegistryDto): OrgTableRegistryResponseDto
}
