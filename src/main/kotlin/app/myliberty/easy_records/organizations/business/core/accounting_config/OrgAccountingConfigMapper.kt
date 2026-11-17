package app.myliberty.easy_records.organizations.business.accounting_config

import app.myliberty.easy_records.organizations.business.accounting_config.api.OrgAccountingConfigDto
import app.myliberty.easy_records.organizations.business.accounting_config.api.OrgAccountingConfigInsertDto
import app.myliberty.easy_records.organizations.business.accounting_config.api.OrgAccountingConfigResponseDto
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface OrgAccountingConfigMapper {

    fun toDomainDto(orgAccountingConfigEntity: OrgAccountingConfigEntity): OrgAccountingConfigDto

    fun toEntity(dto: OrgAccountingConfigDto): OrgAccountingConfigEntity

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    fun toEntity(insertDto: OrgAccountingConfigInsertDto): OrgAccountingConfigEntity

    fun toResponseDto(dto: OrgAccountingConfigDto): OrgAccountingConfigResponseDto
}
