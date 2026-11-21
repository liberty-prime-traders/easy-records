package app.myliberty.easy_records.organizations.business.retail.unitgroup

import app.myliberty.easy_records.organizations.business.retail.unitgroup.api.UnitGroupDto
import app.myliberty.easy_records.organizations.business.retail.unitgroup.api.UnitGroupInsertDto
import app.myliberty.easy_records.organizations.business.retail.unitgroup.api.UnitGroupResponseDto
import app.myliberty.easy_records.platform.business.sysuser.api.CreatorFullName
import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = LibertyMapperConfig::class)
interface UnitGroupMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "systemDefined", constant = "false")
    @Mapping(target = "nameKey", ignore = true)
    fun toEntity(unitGroupInsertDto: UnitGroupInsertDto): UnitGroupEntity

    fun toDomainDto(unitGroupEntity: UnitGroupEntity): UnitGroupDto

    @Mapping(target = "nameKey", ignore = true)
    fun toEntity(unitGroupDto: UnitGroupDto): UnitGroupEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [CreatorFullName::class])
    fun toResponseDto(unitGroupDto: UnitGroupDto): UnitGroupResponseDto
}
