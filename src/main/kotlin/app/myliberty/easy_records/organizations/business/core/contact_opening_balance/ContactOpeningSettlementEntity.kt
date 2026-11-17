package app.myliberty.easy_records.organizations.business.contact_opening_balance

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.organizations.business.contact_opening_balance.api.ContactSubledgerDirection
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.ImmutableEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = TableNames.CONTACT_OPENING_SETTLEMENT)
@HasReference(tableName = TableName.CONTACT_OPENING_SETTLEMENT)
class ContactOpeningSettlementEntity(

    @Column(name = "contact_reference_number", nullable = false, updatable = false)
    var contactReferenceNumber: String,

    @Column(name = "subledger_direction", nullable = false, updatable = false)
    @Convert(converter = ContactSubledgerDirectionConverter::class)
    var subledgerDirection: ContactSubledgerDirection,

    @Column(name = "payment_account_code", nullable = false, updatable = false)
    var paymentAccountCode: String,

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4)
    var amount: BigDecimal,

    @Column(name = "settlement_date", nullable = false, updatable = false)
    var settlementDate: LocalDate,

    @Column(name = "note", updatable = false)
    var note: String?

) : ImmutableEntity() {

    fun requiredId(): UUID = requireNotNull(id) { "ContactOpeningSettlementEntity is missing its id" }
}
