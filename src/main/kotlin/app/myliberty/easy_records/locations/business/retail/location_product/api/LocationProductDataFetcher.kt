package app.myliberty.easy_records.locations.business.retail.location_product.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.cross_tier.retail.product.search.common.ProductSearchParameters
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductEnricher
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductForPurchaseAssembler
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductForSaleAssembler
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductPagedSearch
import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductRepository
import app.myliberty.easy_records.organizations.business.retail.product.api.ProductStatus
import app.myliberty.easy_records.organizations.business.retail.unitconversion.api.UnitConversionGraphFacade
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class LocationProductDataFetcher(
    private val locationProductRepository: LocationProductRepository,
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
    private val locationProductPagedSearch: LocationProductPagedSearch,
    private val locationProductEnricher: LocationProductEnricher,
    private val locationProductForSaleAssembler: LocationProductForSaleAssembler,
    ) {

    fun searchWithParameters(
        pageRequest: PageRequest<ProductSearchParameters, String>
    ): PageResponse<LocationProductResponseDto, String> {
        val page = locationProductPagedSearch.searchWithParameters(pageRequest)
        return PageResponse(
            currentCursor = page.currentCursor,
            hasMore = page.hasMore,
            contents = locationProductEnricher.convertToResponseDto(page.contents.toList()),
            requireClientSideFilter = page.requireClientSideFilter,
        )
    }

    fun searchAvailable(
        pageRequest: PageRequest<LocationProductSearchParameters, String>
    ): PageResponse<LocationProductWithAvailability, String> {
        val page = locationProductPagedSearch.searchWithParameters(toActiveProductSearch(pageRequest, requireDefaultSalePrice = true))
        return PageResponse(
            currentCursor = page.currentCursor,
            hasMore = page.hasMore,
            contents = locationProductForSaleAssembler.assemble(page.contents.toList()),
            requireClientSideFilter = page.requireClientSideFilter,
        )
    }

    fun searchForPurchase(
        pageRequest: PageRequest<LocationProductSearchParameters, String>
    ): PageResponse<LocationProductForPurchaseDto, String> {
        val page = locationProductPagedSearch.searchWithParameters(toActiveProductSearch(pageRequest))
        return PageResponse(
            currentCursor = page.currentCursor,
            hasMore = page.hasMore,
            contents = LocationProductForPurchaseAssembler.assemble(page.contents.toList()),
            requireClientSideFilter = page.requireClientSideFilter,
        )
    }

    fun generateFormattedQuery(pageRequest: PageRequest<ProductSearchParameters, String>): String {
        return locationProductPagedSearch.generateFormattedQuery(pageRequest)
    }

    private fun toActiveProductSearch(
        pageRequest: PageRequest<LocationProductSearchParameters, String>,
        requireDefaultSalePrice: Boolean = false,
    ): PageRequest<ProductSearchParameters, String> = PageRequest(
        previousCursor = pageRequest.previousCursor,
        requestedSize = pageRequest.requestedSize,
        parameters = ProductSearchParameters(
            searchText = pageRequest.parameters.searchText,
            excludeIds = pageRequest.parameters.excludeIds,
            statusList = setOf(ProductStatus.ACTIVE),
            requireDefaultSalePrice = requireDefaultSalePrice,
            searchStrategy = pageRequest.parameters.searchStrategy
        )
    )

    fun findSummaryByIds(ids: Collection<UUID>): Map<UUID, LocationProductSummaryDto> =
        ids.chunked(SUMMARY_CHUNK_SIZE)
            .flatMap { locationProductRepository.findSummariesByIdIn(it) }
            .associate { summaryView ->
                summaryView.id to LocationProductSummaryDto(
                    id = summaryView.id,
                    referenceNumber = summaryView.referenceNumber,
                    orgProductId = summaryView.orgProductId,
                    productName = summaryView.productName,
                    productGroupName = summaryView.productGroupName,
                    baseUnitId = summaryView.baseUnitId,
                )
            }

    fun findInactiveIds(ids: Set<UUID>): Set<UUID> =
        ids.asSequence().chunked(SUMMARY_CHUNK_SIZE)
            .flatMap { locationProductRepository.findIdsByIdInAndStatusNot(it, ProductStatus.ACTIVE) }
            .toSet()

    fun findIdsByReferenceNumbers(referenceNumbers: Collection<String>): Map<String, UUID> =
        locationProductRepository.findReferenceViewsByReferenceNumberIn(referenceNumbers)
            .associate { it.referenceNumber to it.id }

    fun getDefaultSalePrices(locationProductIds: Collection<UUID>): Map<UUID, BigDecimal?> =
        locationProductRepository.findDefaultSalePriceViewsByIdIn(locationProductIds)
            .associate { it.id to it.defaultSalePrice }

    fun getBaseUnitIds(locationProductIds: Collection<UUID>): Map<UUID, UUID> =
        locationProductRepository.findIdentityViewsByIdIn(locationProductIds)
            .associate { it.id to it.baseUnitId }

    fun getBaseUnitIdsByOrgProductIds(orgProductIds: Collection<UUID>): Map<UUID, UUID> =
        locationProductRepository.findIdentityViewsByOrgProductIdIn(orgProductIds)
            .associate { it.orgProductId to it.baseUnitId }

    fun getOrgProductIds(locationProductIds: Collection<UUID>): Map<UUID, UUID> =
        locationProductRepository.findIdentityViewsByIdIn(locationProductIds)
            .associate { it.id to it.orgProductId }

    fun findIdentityByOrgProductId(orgProductId: UUID): LocationProductIdentityDto {
        val identityView = locationProductRepository.findIdentityViewsByOrgProductIdIn(listOf(orgProductId)).firstOrNull()
            ?: throw LibertyGenericException("No location product found for product $orgProductId")
        return LocationProductIdentityDto(locationProductId = identityView.id, orgProductId = identityView.orgProductId)
    }

    fun findIdentitiesByOrgProductIds(orgProductIds: Collection<UUID>): Map<UUID, LocationProductIdentityDto> =
        locationProductRepository.findIdentityViewsByOrgProductIdIn(orgProductIds)
            .associate { identityView ->
                identityView.orgProductId to LocationProductIdentityDto(
                    locationProductId = identityView.id,
                    orgProductId = identityView.orgProductId
                )
            }

    fun getStorageUnitsPerUserProvidedUnit(productUnitRequests: List<LocationProductUnitRequestDto>): Map<UUID, Long> =
        getStorageUnitsByRequest(productUnitRequests).mapKeys { it.key.locationProductId }

    /** Keyed by the request itself, so one product can be asked about several units in a single lookup. */
    fun getStorageUnitsByRequest(productUnitRequests: List<LocationProductUnitRequestDto>): Map<LocationProductUnitRequestDto, Long> {
        if (productUnitRequests.isEmpty()) return emptyMap()
        val baseUnitIds = getBaseUnitIds(productUnitRequests.map { it.locationProductId })
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        return productUnitRequests.associateWith { (locationProductId, unitId) ->
            val baseUnitId = baseUnitIds.getValue(locationProductId)
            if (unitConversionGraph.getStorageUnitId(unitId) != unitConversionGraph.getStorageUnitId(baseUnitId)) {
                throw LibertyGenericException(UNIT_OUTSIDE_PRODUCT_GROUP)
            }
            unitConversionGraph.getSizeInRoot(unitId)
        }
    }

    companion object {
        const val UNIT_OUTSIDE_PRODUCT_GROUP = "The unit must belong to the same unit group as the product's base unit"

        private const val SUMMARY_CHUNK_SIZE = 10_000
    }
}
