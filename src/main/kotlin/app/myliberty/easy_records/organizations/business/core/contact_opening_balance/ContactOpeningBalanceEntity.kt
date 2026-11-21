package app.myliberty.easy_records.organizations.business.core.contact_opening_balance

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.api.ContactSubledgerDirection
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.ImmutableEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.math.BigDecimal

@Entity
@Table(name = TableNames.CONTACT_OPENING_BALANCE)
@HasReference(tableName = TableName.CONTACT_OPENING_BALANCE)
class ContactOpeningBalanceEntity(

    @Column(name = "contact_reference_number", nullable = false, updatable = false)
    var contactReferenceNumber: String,

    @Column(name = "subledger_direction", nullable = false, updatable = false)
    @Convert(converter = ContactSubledgerDirectionConverter::class)
    var subledgerDirection: ContactSubledgerDirection,

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4)
    var amount: BigDecimal

) : ImmutableEntity()
