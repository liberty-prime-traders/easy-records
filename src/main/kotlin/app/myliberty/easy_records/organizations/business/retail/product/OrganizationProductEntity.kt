package app.myliberty.easy_records.organizations.business.retail.product

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table
import app.myliberty.easy_records.organizations.business.retail.product.api.ProductStatus
import app.myliberty.easy_records.organizations.business.retail.product.api.ProductStatusConverter
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.annotations.Generated
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import org.hibernate.generator.EventType
import java.util.UUID

@Audited
@Entity
@EntityListeners(OrganizationProductNameKeyListener::class)
@Table(name = TableNames.PRODUCT)
@HasReference(tableName = TableName.PRODUCT)
class OrganizationProductEntity(

    @Column(name = "name", nullable = false)
    var productName: String,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "product_group_id", nullable = false)
    var productGroupId: UUID,

    @Column(name = "base_unit_id", updatable = false)
    var baseUnitId: UUID

): HasReferenceEntity() {

    @NotAudited
    @Column(name = "name_key", nullable = false)
    var nameKey: String = StringUtils.alphanumericKey(productName)

    @Convert(converter = ProductStatusConverter::class)
    @Column(name = "status", nullable = false)
    var status: ProductStatus = ProductStatus.ACTIVE

    @NotAudited
    @Generated(event = [EventType.INSERT, EventType.UPDATE])
    @Column(name = "product_group_name", insertable = false, updatable = false)
    var productGroupName: String = ""
}
