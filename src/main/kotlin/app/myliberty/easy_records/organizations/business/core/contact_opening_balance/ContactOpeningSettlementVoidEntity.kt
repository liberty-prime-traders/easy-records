package app.myliberty.easy_records.organizations.business.core.contact_opening_balance

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
@Table(name = TableNames.CONTACT_OPENING_SETTLEMENT_VOID)
@HasReference(tableName = TableName.CONTACT_OPENING_SETTLEMENT_VOID)
class ContactOpeningSettlementVoidEntity(

    @Column(name = "settlement_id", nullable = false, updatable = false, unique = true)
    var settlementId: UUID,

    @Column(name = "void_date", nullable = false, updatable = false)
    var voidDate: LocalDate,

    @Column(name = "reason", updatable = false)
    var reason: String?

) : ImmutableEntity()
