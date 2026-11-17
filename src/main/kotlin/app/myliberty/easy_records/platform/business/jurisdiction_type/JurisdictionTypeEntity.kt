package app.myliberty.easy_records.platform.business.jurisdiction_type

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
@Table(name = TableNames.JURISDICTION_TYPE)
@HasReference(tableName = TableName.JURISDICTION_TYPE)
class JurisdictionTypeEntity(

    @Column(name = "name", length = 100, nullable = false)
    var name: String,

    @Column(name = "description", length = 500)
    var description: String? = null

) : HasReferenceEntity()
