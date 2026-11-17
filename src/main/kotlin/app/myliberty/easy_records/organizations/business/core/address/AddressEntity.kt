package app.myliberty.easy_records.organizations.business.address

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
@Table(name = TableNames.ADDRESS)
@HasReference(tableName = TableName.ADDRESS)
class AddressEntity(

    @Column(name = "line1", length = 100)
    var line1: String? = null,

    @Column(name = "line2", length = 50)
    var line2: String? = null,

    @Column(name = "line3", length = 30)
    var line3: String? = null,

    @Column(name = "state", length = 100)
    var state: String? = null,

    @Column(name = "postal_code", length = 10)
    var postalCode: String? = null,

    @Column(name = "country", length = 100)
    var country: String? = null

): HasReferenceEntity()
