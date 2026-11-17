package app.myliberty.easy_records.organizations.business.core.ledger

import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper

@Mapper(config = RtsMapperConfig::class)
interface LedgerMapper {

    fun toGroupDto(ledgerEntryGroupEntity: LedgerEntryGroupEntity): LedgerEntryGroupDto
}
