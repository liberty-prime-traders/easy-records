package app.myliberty.easy_records.locations.business.retail.location_product.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductCache
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductEntity
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductInsertDto
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductMapper
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductRepository
import app.myliberty.easy_records.organizations.business.retail.product.api.ProductStatus
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class LocationProductSyncService(
    private val locationProductRepository: LocationProductRepository,
    private val locationProductCache: LocationProductCache,
    private val locationProductMapper: LocationProductMapper
) {

    @TransactionalOnLocationSchema
    fun syncUpsert(syncDto: LocationProductSyncDto): Boolean {
        val existing = locationProductRepository.findByOrgProductId(syncDto.orgProductId)

        if (existing != null) {
            // Stock is counted in the storage unit of the base unit's group, so a different base unit would re-meaning every stored quantity.
            if (existing.baseUnitId != syncDto.baseUnitId) throw RtsGenericException(BASE_UNIT_CANNOT_CHANGE)
            if (fieldsMatch(existing, syncDto)) return false

            val dto = locationProductMapper.toDomainDto(existing)
            val updatedStatus = if (existing.status == ProductStatus.ACTIVE) syncDto.status else dto.status
            locationProductCache.save(dto.copy(
                productName = syncDto.productName,
                description = syncDto.description,
                productGroupName = syncDto.productGroupName,
                categoryId = syncDto.categoryId,
                baseUnitId = syncDto.baseUnitId,
                lastSyncedAt = OffsetDateTime.now(),
                status = updatedStatus
            ))
            return true
        }

        locationProductCache.create(
            LocationProductInsertDto(
                orgProductId = syncDto.orgProductId,
                productName = syncDto.productName,
                description = syncDto.description,
                productGroupName = syncDto.productGroupName,
                categoryId = syncDto.categoryId,
                baseUnitId = syncDto.baseUnitId,
                status = syncDto.status,
                lastSyncedAt = OffsetDateTime.now()
            )
        )
        return true
    }

    companion object {
        const val BASE_UNIT_CANNOT_CHANGE = "A product's base unit cannot change once it is stocked at a location"
    }

    private fun fieldsMatch(existing: LocationProductEntity, syncDto: LocationProductSyncDto): Boolean =
        StringUtils.isEquivalent(existing.productName, syncDto.productName)
            && StringUtils.isEquivalent(existing.description, syncDto.description)
            && StringUtils.isEquivalent(existing.productGroupName, syncDto.productGroupName)
            && existing.categoryId == syncDto.categoryId
            && existing.baseUnitId == syncDto.baseUnitId
            && existing.status == syncDto.status
}
