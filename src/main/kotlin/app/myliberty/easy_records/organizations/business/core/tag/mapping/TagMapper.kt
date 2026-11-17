package app.myliberty.easy_records.organizations.business.core.tag.mapping

import app.myliberty.easy_records.organizations.business.core.tag.TagEntity
import app.myliberty.easy_records.organizations.business.core.tag.api.TagDto
import app.myliberty.easy_records.organizations.business.core.tag.api.TagInsertDto
import app.myliberty.easy_records.organizations.business.core.tag.api.TagResponseDto
import app.myliberty.easy_records.platform.business.sysuser.api.CreatorFullName
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface TagMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(tagInsertDto: TagInsertDto): TagEntity

    fun toDomainDto(tagEntity: TagEntity): TagDto

    fun toEntity(tagDto: TagDto): TagEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [CreatorFullName::class])
    fun toResponseDto(tagDto: TagDto): TagResponseDto
}
