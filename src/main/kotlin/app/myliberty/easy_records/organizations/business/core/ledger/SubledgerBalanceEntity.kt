package app.myliberty.easy_records.organizations.business.core.ledger

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.model.HasCreatorEntity
import app.myliberty.easy_records.util.model.TableNames
import java.math.BigDecimal

@Entity
@Table(name = TableNames.SUBLEDGER_BALANCE)
class SubledgerBalanceEntity(

    @Column(name = "contact_reference_number", nullable = false, updatable = false)
    var contactReferenceNumber: String,

    @Column(name = "running_payable", nullable = false, precision = 19, scale = 4)
    var runningPayable: BigDecimal,

    @Column(name = "running_receivable", nullable = false, precision = 19, scale = 4)
    var runningReceivable: BigDecimal

) : HasCreatorEntity()
