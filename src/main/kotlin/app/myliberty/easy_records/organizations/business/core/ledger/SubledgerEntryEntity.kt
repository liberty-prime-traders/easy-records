package app.myliberty.easy_records.organizations.business.core.ledger

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.model.HasCreatorEntity
import app.myliberty.easy_records.util.model.TableNames
import java.math.BigDecimal

@Entity
@Table(name = TableNames.SUBLEDGER_ENTRY)
class SubledgerEntryEntity(

    @Column(name = "group_reference_number", nullable = false, updatable = false)
    var groupReferenceNumber: String,

    @Column(name = "contact_reference_number", nullable = false, updatable = false)
    var contactReferenceNumber: String,

    @Column(name = "payable_amount", nullable = false, updatable = false, precision = 19, scale = 4)
    var payableAmount: BigDecimal,

    @Column(name = "receivable_amount", nullable = false, updatable = false, precision = 19, scale = 4)
    var receivableAmount: BigDecimal,

    @Column(name = "running_payable", nullable = false, updatable = false, precision = 19, scale = 4)
    var runningPayable: BigDecimal,

    @Column(name = "running_receivable", nullable = false, updatable = false, precision = 19, scale = 4)
    var runningReceivable: BigDecimal

) : HasCreatorEntity()
