package app.myliberty.easy_records.platform.business.authorization_pass

import app.myliberty.easy_records.platform.business.authorization_pass.api.AuthorizationPassResponseDto
import app.myliberty.easy_records.platform.business.sysuser.api.CreatorFullName
import app.myliberty.easy_records.platform.business.sysuser.api.NullableFullName
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface AuthorizationPassMapper {

    @Mapping(source = "assignedToId", target = "assignedTo", qualifiedBy = [NullableFullName::class])
    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [CreatorFullName::class])
    fun toResponseDto(authorizationPassEntity: AuthorizationPassEntity): AuthorizationPassResponseDto
}
