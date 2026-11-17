package app.myliberty.easy_records.organizations.business.retail.unitgroup

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited

@Audited
@Entity
@EntityListeners(UnitGroupNameKeyListener::class)
@Table(name = TableNames.UNIT_GROUP)
@HasReference(tableName = TableName.UNIT_GROUP)
class UnitGroupEntity(

    @Column(name = "code", length = 5, updatable = false)
    var code: String? = null,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "system_defined", nullable = false, updatable = false)
    var systemDefined: Boolean = false

): HasReferenceEntity() {

    @NotAudited
    @Column(name = "name_key", nullable = false)
    var nameKey: String = StringUtils.alphanumericKey(name)
}
