package app.myliberty.easy_records.locations.business.location_product

import app.myliberty.easy_records.locations.business.location_product.api.LocationProductUpdateDto
import app.myliberty.easy_records.util.exceptions.RtsGenericException

object LocationProductValidator {

  fun validateProductUpdate(locationProductUpdateDto: LocationProductUpdateDto) {
    locationProductUpdateDto.minStockLevel?.ifPresent { level ->
      if (level < 0) {
        throw RtsGenericException("Stock level cannot be negative")
      }
    }
  }
}
