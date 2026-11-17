package app.myliberty.easy_records.organizations.business.jobtitle

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
@Table(name = TableNames.JOB_TITLE)
@HasReference(tableName = TableName.JOB_TITLE)
class JobTitleEntity(
    @Column(name = "value")
    var value: String
) : HasReferenceEntity()
