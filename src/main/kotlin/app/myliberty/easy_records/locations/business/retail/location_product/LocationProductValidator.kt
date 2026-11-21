package app.myliberty.easy_records.locations.business.retail.location_product

import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductUpdateDto
import app.myliberty.easy_records.util.exceptions.LibertyGenericException

object LocationProductValidator {

  fun validateProductUpdate(locationProductUpdateDto: LocationProductUpdateDto) {
    locationProductUpdateDto.minStockLevel?.ifPresent { level ->
      if (level < 0) {
        throw LibertyGenericException("Stock level cannot be negative")
      }
    }
  }
}
