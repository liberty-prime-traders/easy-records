package app.myliberty.easy_records.locations.business.stock.api.read

import app.myliberty.easy_records.locations.business.stock.StockEntryEntity
import app.myliberty.easy_records.locations.business.stock.api.StockEntryStatus
import app.myliberty.easy_records.organizations.business.stock_item_source.api.StockItemSource
import java.math.BigDecimal
import java.util.UUID

data class StockEntryDto(
    val id: UUID,
    val referenceNumber: String,
    val locationProductId: UUID,
    val sourceType: StockItemSource?,
    val documentReference: String?,
    val detailReference: String?,
    val storageQuantityRemaining: BigDecimal,
    val costRemaining: BigDecimal,
    val status: StockEntryStatus,
) {
    companion object {
        fun of(stockEntryEntity: StockEntryEntity) = StockEntryDto(
            id = stockEntryEntity.id!!,
            referenceNumber = stockEntryEntity.requiredReference(),
            locationProductId = stockEntryEntity.locationProductId,
            sourceType = stockEntryEntity.sourceType,
            documentReference = stockEntryEntity.documentReference,
            detailReference = stockEntryEntity.detailReference,
            storageQuantityRemaining = stockEntryEntity.storageQuantityRemaining,
            costRemaining = stockEntryEntity.costRemaining,
            status = stockEntryEntity.status,
        )
    }
}
