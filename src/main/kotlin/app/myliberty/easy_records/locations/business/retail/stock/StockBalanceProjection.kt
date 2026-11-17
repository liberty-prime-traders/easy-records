package app.myliberty.easy_records.locations.business.stock

import java.math.BigDecimal
import java.util.UUID

interface StockBalanceProjection {
  fun getLocationProductId(): UUID
  fun getStorageRemainingQuantity(): BigDecimal
}
