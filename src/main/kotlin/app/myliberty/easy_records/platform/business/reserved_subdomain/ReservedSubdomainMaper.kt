package app.myliberty.easy_records.platform.business.reserved_subdomain

import app.myliberty.easy_records.platform.business.reserved_subdomain.api.ReservedSubdomainDto
import app.myliberty.easy_records.platform.business.sysuser.api.CreatorFullName
import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = LibertyMapperConfig::class)
interface ReservedSubdomainMaper {

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [CreatorFullName::class])
    fun toDto(entity: ReservedSubdomainEntity): ReservedSubdomainDto
}
