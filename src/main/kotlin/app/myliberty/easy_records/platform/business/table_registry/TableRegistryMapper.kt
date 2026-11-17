package app.myliberty.easy_records.platform.business.table_registry

import app.myliberty.easy_records.platform.business.db_version.api.DbVersionNumber
import app.myliberty.easy_records.platform.business.db_version.api.DbVersionService
import app.myliberty.easy_records.platform.business.table_registry.api.TableRegistryDto
import app.myliberty.easy_records.platform.business.table_registry.api.TableRegistryResponseDto
import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(
    config = LibertyMapperConfig::class,
    uses = [DbVersionService::class]
)
interface TableRegistryMapper {

    fun toDomainDto(tableRegistryEntity: TableRegistryEntity): TableRegistryDto

    fun toEntity(dto: TableRegistryDto): TableRegistryEntity

    @Mapping(source = "minimumVersionId", target = "minimumVersion", qualifiedBy = [DbVersionNumber::class])
    fun toDto(dto: TableRegistryDto): TableRegistryResponseDto
}
