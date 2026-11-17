package app.myliberty.easy_records.organizations.business.core.manual_journal

import app.myliberty.easy_records.organizations.business.core.account.api.AccountType
import app.myliberty.easy_records.organizations.business.core.account.api.AccountNameAndTypeDto
import app.myliberty.easy_records.organizations.business.core.manual_journal.api.ManualJournalLineResponseDto
import app.myliberty.easy_records.organizations.business.core.manual_journal.api.ManualJournalResponseDto
import app.myliberty.easy_records.platform.business.sysuser.api.CreatorFullName
import app.myliberty.easy_records.util.business.mappers.RtsMapperConfig
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named

@Mapper(config = RtsMapperConfig::class)
abstract class ManualJournalResponseMapper {

    @Mapping(source = "manualJournal.createdById", target = "postedBy", qualifiedBy = [CreatorFullName::class])
    @Mapping(source = "manualJournal.createdOn", target = "postedOn")
    abstract fun toResponseDto(
        manualJournal: ManualJournalEntity,
        lines: List<ManualJournalLineEntity>,
        reversalOfReferenceNumber: String?,
        reversedByReferenceNumber: String?,
        @Context accountsByCode: Map<String, AccountNameAndTypeDto>
    ): ManualJournalResponseDto

    @Mapping(source = "accountCode", target = "accountName", qualifiedByName = [ACCOUNT_NAME])
    @Mapping(source = "accountCode", target = "accountType", qualifiedByName = [ACCOUNT_TYPE])
    abstract fun toLineResponseDto(
        manualJournalLine: ManualJournalLineEntity,
        @Context accountsByCode: Map<String, AccountNameAndTypeDto>
    ): ManualJournalLineResponseDto

    @Named(ACCOUNT_NAME)
    protected fun accountName(accountCode: String, @Context accountsByCode: Map<String, AccountNameAndTypeDto>): String =
        accountsByCode.getValue(accountCode).name

    @Named(ACCOUNT_TYPE)
    protected fun accountType(accountCode: String, @Context accountsByCode: Map<String, AccountNameAndTypeDto>): AccountType =
        accountsByCode.getValue(accountCode).accountType

    companion object {
        const val ACCOUNT_NAME = "accountName"
        const val ACCOUNT_TYPE = "accountType"
    }
}
