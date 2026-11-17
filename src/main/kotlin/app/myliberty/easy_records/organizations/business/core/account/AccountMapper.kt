package app.myliberty.easy_records.organizations.business.core.account

import app.myliberty.easy_records.organizations.business.core.account.api.AccountInsertDto
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping


@Mapper(config = RtsMapperConfig::class)
interface AccountMapper {

    fun toDomainDto(accountEntity: AccountEntity): AccountDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "currentBalance", constant = "0.0")
    @Mapping(target = "balanceUpdatedAt", ignore = true)
    @Mapping(target = "currencyCode", defaultValue = "KES")
    @Mapping(target = "accountIsActive", constant = "true")
    fun toEntity(insertDto: AccountInsertDto): AccountEntity
}
