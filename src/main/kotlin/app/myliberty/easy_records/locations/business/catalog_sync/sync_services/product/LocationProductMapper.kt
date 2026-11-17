package app.myliberty.easy_records.locations.business.catalog_sync.sync_services.product

import app.myliberty.easy_records.locations.business.location_product.api.LocationProductSyncDto

object LocationProductMapper {

  fun toSyncDto(syncData: ProductSyncData): LocationProductSyncDto {
    return LocationProductSyncDto(
      orgProductId = syncData.orgProductId,
      productName = syncData.productName,
      description = syncData.description,
      productGroupName = syncData.productGroupName,
      status = syncData.status,
      categoryId = syncData.categoryId,
      baseUnitId = syncData.baseUnitId
    )
  }
}
