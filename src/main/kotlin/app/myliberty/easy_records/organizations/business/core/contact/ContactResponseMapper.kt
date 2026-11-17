package app.myliberty.easy_records.organizations.business.core.contact

import app.myliberty.easy_records.organizations.business.core.contact.api.ContactDto
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactResponseDto
import app.myliberty.easy_records.organizations.business.core.ledger.api.SubledgerBalanceDto
import app.myliberty.easy_records.platform.business.sysuser.api.CreatorFullName
import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = LibertyMapperConfig::class)
interface ContactResponseMapper {

    @Mapping(source = "contactDto.createdById", target = "createdBy", qualifiedBy = [CreatorFullName::class])
    @Mapping(source = "contactDto.identity.identityType", target = "identityType")
    @Mapping(source = "contactDto.identity.displayName", target = "fullName")
    @Mapping(source = "subledgerBalanceDto.receivable", target = "receivableBalance")
    @Mapping(source = "subledgerBalanceDto.payable", target = "payableBalance")
    @Mapping(source = "subledgerBalanceDto.netBalance", target = "netBalance")
    @Mapping(source = "subledgerBalanceDto.balanceEffect", target = "balanceEffect")
    @Mapping(source = "openingBalanceDto.netBalance", target = "netOpeningBalance")
    @Mapping(source = "openingBalanceDto.balanceEffect", target = "netOpeningBalanceEffect")
    fun toResponseDto(
        contactDto: ContactDto,
        subledgerBalanceDto: SubledgerBalanceDto,
        openingBalanceDto: SubledgerBalanceDto
    ): ContactResponseDto
}
