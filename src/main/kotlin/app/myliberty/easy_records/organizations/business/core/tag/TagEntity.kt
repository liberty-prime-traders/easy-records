package app.myliberty.easy_records.organizations.business.core.tag

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.organizations.business.core.tag.api.CategoryType
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited

@Audited
@Entity
@Table(name = TableNames.TAG)
@HasReference(tableName = TableName.TAG)
class TagEntity(

    @Column(name = "category")
    @Convert(converter = CategoryTypeConverter::class)
    var category: CategoryType,

    @Column(name = "tag_name", nullable = false)
    var tagName: String,

    @Column(name = "description")
    var description: String? = null

): HasReferenceEntity()
