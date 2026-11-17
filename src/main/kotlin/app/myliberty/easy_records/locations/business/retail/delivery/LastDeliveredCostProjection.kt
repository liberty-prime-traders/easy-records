package app.myliberty.easy_records.locations.business.retail.delivery

import java.math.BigDecimal
import java.util.UUID

interface LastDeliveredCostProjection {
  val locationProductId: UUID
  val unitCost: BigDecimal
  val storageUnitsPerUserProvidedUnit: Long
}
