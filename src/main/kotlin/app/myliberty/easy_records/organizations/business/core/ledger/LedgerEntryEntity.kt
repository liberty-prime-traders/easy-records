package app.myliberty.easy_records.organizations.business.core.ledger

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.organizations.business.core.account.api.EntryType
import app.myliberty.easy_records.organizations.business.core.account.api.EntryTypeConverter
import app.myliberty.easy_records.util.model.HasCreatorEntity
import app.myliberty.easy_records.util.model.TableNames
import java.math.BigDecimal

@Entity
@Table(name = TableNames.LEDGER_ENTRY)
class LedgerEntryEntity(

    @Column(name = "group_reference_number", nullable = false, updatable = false)
    var groupReferenceNumber: String,

    @Column(name = "account_code", nullable = false, updatable = false, length = 50)
    var accountCode: String,

    @Convert(converter = EntryTypeConverter::class)
    @Column(name = "entry_type", nullable = false, updatable = false, length = 5)
    var entryType: EntryType,

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4)
    var amount: BigDecimal

) : HasCreatorEntity()
