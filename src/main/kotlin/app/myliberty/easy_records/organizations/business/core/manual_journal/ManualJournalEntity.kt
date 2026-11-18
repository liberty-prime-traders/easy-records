package app.myliberty.easy_records.organizations.business.core.manual_journal

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.ImmutableEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = TableNames.MANUAL_JOURNAL)
@HasReference(tableName = TableName.MANUAL_JOURNAL)
class ManualJournalEntity(

    @Column(name = "summary", nullable = false, updatable = false, columnDefinition = "TEXT")
    var summary: String,

    @Column(name = "details", updatable = false, columnDefinition = "TEXT")
    var details: String? = null,

    @Column(name = "posting_date", nullable = false, updatable = false)
    var postingDate: LocalDate,

    @Column(name = "reversal_of_manual_journal_id", updatable = false, unique = true)
    var reversalOfManualJournalId: UUID? = null

) : ImmutableEntity() {

    fun requiredId(): UUID = requireNotNull(id) { "ManualJournalEntity is missing its id" }
}
