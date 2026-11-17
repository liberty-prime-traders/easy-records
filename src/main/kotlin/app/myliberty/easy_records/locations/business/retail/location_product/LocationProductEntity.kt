package app.myliberty.easy_records.locations.business.retail.location_product

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.organizations.business.retail.product.api.ProductStatusConverter
import app.myliberty.easy_records.organizations.business.retail.product.api.ProductStatus
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.LOCATION_PRODUCT)
@HasReference(tableName = TableName.LOCATION_PRODUCT)
class LocationProductEntity(

  @Column(name = "product_id", nullable = false, unique = true)
  var orgProductId: UUID,

  @Column(name = "name", nullable = false, length = 100)
  var productName: String,

  @Column(name = "description", length = 500)
  var description: String? = null,

  @Column(name = "product_group_name", nullable = false, length = 100)
  var productGroupName: String,

  @Column(name = "category_id", nullable = false)
  var categoryId: UUID,

  @Column(name = "base_unit_id", nullable = false)
  var baseUnitId: UUID,

  @Column(name = "default_sale_price", precision = 28, scale = 10)
  var defaultSalePrice: BigDecimal? = null,

  @Column(name = "min_stock_level")
  var minStockLevel: Int? = null,

  @Column(name = "last_purchase_price", precision = 19, scale = 4)
  var lastPurchasePrice: BigDecimal? = null,

  @Convert(converter = ProductStatusConverter::class)
  @Column(name = "status", nullable = false, length = 5)
  var status: ProductStatus = ProductStatus.ACTIVE,

  @NotAudited
  @Column(name = "last_synced_at")
  var lastSyncedAt: OffsetDateTime? = null

) : HasReferenceEntity()
