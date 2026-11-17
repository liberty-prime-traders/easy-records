package app.myliberty.easy_records.organizations.business.core.ledger

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerSourceType
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = TableNames.LEDGER_ENTRY_GROUP)
@HasReference(tableName = TableName.LEDGER_ENTRY_GROUP)
class LedgerEntryGroupEntity(

    @Column(name = "source_reference_number", nullable = false, updatable = false)
    var sourceReferenceNumber: String,

    @Convert(converter = LedgerSourceTypeConverter::class)
    @Column(name = "source_type", nullable = false, updatable = false, length = 5)
    var sourceType: LedgerSourceType,

    @Column(name = "source_location_id", updatable = false)
    var sourceLocationId: UUID? = null,

    @Column(name = "fiscal_period_id", nullable = false, updatable = false)
    var fiscalPeriodId: UUID,

    @Column(name = "posted_on", nullable = false, updatable = false)
    var postedOn: Instant

) : HasReferenceEntity()
