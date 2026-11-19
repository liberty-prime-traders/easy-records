package app.myliberty.easy_records.organizations.business.core.ledger

import app.myliberty.easy_records.util.business.mappers.LibertyMapperConfig
import org.mapstruct.Mapper

@Mapper(config = LibertyMapperConfig::class)
interface LedgerMapper {

    fun toGroupDto(ledgerEntryGroupEntity: LedgerEntryGroupEntity): LedgerEntryGroupDto
}
