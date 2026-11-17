package app.myliberty.easy_records.organizations.business.core.payment_method

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited

@Audited
@Entity
@Table(name = TableNames.PAYMENT_METHOD)
@HasReference(tableName = TableName.PAYMENT_METHOD)
class PaymentMethodEntity (

    @Column(name = "name", unique = true, length = 100)
    var name: String? = null,

    @Column(name = "description", length = 1000)
    var description: String? = null,

    @Column(name = "account_code", nullable = false, length = 50)
    var accountCode: String? = null

): HasReferenceEntity()
