package app.myliberty.easy_records.platform.business.jurisdiction

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.JURISDICTION)
@HasReference(tableName = TableName.JURISDICTION)
class JurisdictionEntity(

    @Column(name = "name", length = 200, nullable = false)
    var name: String,

    @Column(name = "jurisdiction_type_id", nullable = false)
    var jurisdictionTypeId: UUID,

    @Column(name = "parent_jurisdiction_id")
    var parentJurisdictionId: UUID? = null

) : HasReferenceEntity()
