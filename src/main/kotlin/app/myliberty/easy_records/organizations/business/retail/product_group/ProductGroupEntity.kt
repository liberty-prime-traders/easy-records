package app.myliberty.easy_records.organizations.business.product_group

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
@Table(name = TableNames.PRODUCT_GROUP)
@HasReference(tableName = TableName.PRODUCT_GROUP)
class ProductGroupEntity(

    @Column(name = "group_name", nullable = false)
    var groupName: String,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "category_id", nullable = false)
    var categoryId: UUID

): HasReferenceEntity()
