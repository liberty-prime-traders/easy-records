package app.myliberty.easy_records.organizations.business.core.jobtitle

import app.myliberty.easy_records.organizations.business.core.jobtitle.api.JobTitleInsertDto
import app.myliberty.easy_records.organizations.business.core.jobtitle.api.JobTitleResponseDto
import app.myliberty.easy_records.platform.business.sysuser.api.CreatorFullName
import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = LibertyMapperConfig::class)
interface JobTitleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(titleInsertDto: JobTitleInsertDto): JobTitleEntity

    fun toDomainDto(titleEntity: JobTitleEntity): JobTitleDto

    fun toEntity(titleDto: JobTitleDto): JobTitleEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [CreatorFullName::class])
    fun toDto(titleDto: JobTitleDto): JobTitleResponseDto
}
