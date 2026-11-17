package app.myliberty.easy_records.locations.business.retail.reports.product_purchases

import jakarta.persistence.Tuple
import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.PurchaseDeliveryLineStatus
import app.myliberty.easy_records.util.business.mappers.DateQualifier
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

object ProductPurchasesReportRowMapper {

  fun summaryFromTuple(tuple: Tuple): ProductPurchasesSummaryRawRow = ProductPurchasesSummaryRawRow(
    locationProductId = tuple.get("location_product_id", UUID::class.java),
    categoryId = tuple.get("category_id", UUID::class.java),
    deliveryLineCount = tuple.get("delivery_line_count", Number::class.java).toLong(),
    storageQuantity = tuple.get("storage_quantity", BigDecimal::class.java),
    totalCost = tuple.get("total_cost", BigDecimal::class.java)
  )

  fun deliveryLineFromTuple(tuple: Tuple): ProductPurchasesDeliveryLineRawRow = ProductPurchasesDeliveryLineRawRow(
    id = tuple.get("delivery_line_id", UUID::class.java),
    purchaseReferenceNumber = tuple.get("purchase_reference_number", String::class.java),
    deliveryReferenceNumber = tuple.get("delivery_reference_number", String::class.java),
    deliveredAt = requireNotNull(DateQualifier.toOffsetDateTime(tuple.get("delivered_at", Instant::class.java))),
    supplierId = tuple.get("supplier_id", UUID::class.java),
    unitId = tuple.get("unit_id", UUID::class.java),
    userProvidedQuantityDelivered = tuple.get("user_provided_quantity_delivered", BigDecimal::class.java),
    storageUnitsPerUserProvidedUnit = tuple.get("storage_units_per_user_provided_unit", Number::class.java).toLong(),
    unitCost = tuple.get("unit_cost", BigDecimal::class.java),
    status = PurchaseDeliveryLineStatus.entries.first { it.code == tuple.get("status", String::class.java) }
  )
}
