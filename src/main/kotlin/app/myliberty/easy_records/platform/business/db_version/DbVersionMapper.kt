package app.myliberty.easy_records.platform.business.db_version
import app.myliberty.easy_records.platform.business.db_version.api.DbVersionDto

import app.myliberty.easy_records.platform.business.db_version.api.DbVersionResponseDto
import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = LibertyMapperConfig::class)
interface DbVersionMapper {
    fun toDomainDto(dbVersionEntity: DbVersionEntity): DbVersionDto

    fun toEntity(dto: DbVersionDto): DbVersionEntity

    @Mapping(target = "prevVersion", expression = "java(prevVersion)")
    fun toResponseDto(dto: DbVersionDto, @Context prevVersion: String?): DbVersionResponseDto
}
