package app.myliberty.easy_records.locations.business.retail.catalog_sync.sync_services.product

import app.myliberty.easy_records.organizations.business.retail.product.api.ProductStatus
import java.util.UUID

data class ProductSyncData(
  val orgProductId: UUID,
  val productName: String,
  val description: String?,
  val productGroupName: String,
  val status: ProductStatus,
  val referenceNumber: String,
  val baseUnitId: UUID,
  val categoryId: UUID,
  val revision: Long?
)
