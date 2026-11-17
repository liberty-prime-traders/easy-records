package app.myliberty.easy_records.locations.rest.endpoints

import app.myliberty.easy_records.cross_tier.retail.product.search.common.ProductSearchParameters
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductForPurchaseDto
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductResponseDto
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductService
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductUpdateDto
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductSearchParameters
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductWithAvailability
import app.myliberty.easy_records.locations.business.retail.stock.api.read.StockMovementHistoryBuilder
import app.myliberty.easy_records.locations.business.retail.stock.api.read.StockMovementResponse
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/location-products")
class LocationProductEndpoint(
  private val locationProductService: LocationProductService,
  private val locationProductDataFetcher: LocationProductDataFetcher,
  private val stockMovementHistoryBuilder: StockMovementHistoryBuilder
) {

  @PostMapping("search")
  fun search(
    @RequestBody pageRequest: PageRequest<ProductSearchParameters, String>
  ): PageResponse<LocationProductResponseDto, String> =
    locationProductDataFetcher.searchWithParameters(pageRequest)

  @PostMapping("search-available")
  fun searchAvailable(
    @RequestBody pageRequest: PageRequest<LocationProductSearchParameters, String>
  ): PageResponse<LocationProductWithAvailability, String> =
    locationProductDataFetcher.searchAvailable(pageRequest)

  @PostMapping("search-for-purchase")
  fun searchForPurchase(
    @RequestBody pageRequest: PageRequest<LocationProductSearchParameters, String>
  ): PageResponse<LocationProductForPurchaseDto, String> =
    locationProductDataFetcher.searchForPurchase(pageRequest)

  @PostMapping("search/debug-query")
  fun debugSearchQuery(
    @RequestBody pageRequest: PageRequest<ProductSearchParameters, String>
  ): String =
    locationProductDataFetcher.generateFormattedQuery(pageRequest)

  @PutMapping
  fun updateProduct(@RequestBody dto: LocationProductUpdateDto): LocationProductResponseDto =
    locationProductService.updateProduct(dto)

  @PutMapping("{locationProductId}/deactivate")
  fun deactivateProduct(@PathVariable locationProductId: UUID): LocationProductResponseDto =
    locationProductService.deactivateProduct(locationProductId)

  @PutMapping("{locationProductId}/reactivate")
  fun reactivateProduct(@PathVariable locationProductId: UUID): LocationProductResponseDto =
    locationProductService.reactivateProduct(locationProductId)

  @GetMapping("history")
  fun getMovementHistory(@RequestParam("locationProductId") locationProductId: UUID): Collection<StockMovementResponse> =
    stockMovementHistoryBuilder.build(locationProductId)
}
