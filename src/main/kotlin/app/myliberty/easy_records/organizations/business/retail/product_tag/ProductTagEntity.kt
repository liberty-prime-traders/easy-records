package app.myliberty.easy_records.organizations.business.retail.product_tag

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.ExpirableAssignmentEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.PRODUCT_TAG)
@HasReference(tableName = TableName.PRODUCT_TAG)
class ProductTagEntity(

    @Column(name = "product_id", nullable = false, updatable = false)
    var orgProductId: UUID,

    @Column(name = "tag_id", nullable = false, updatable = false)
    var tagId: UUID

): ExpirableAssignmentEntity()
