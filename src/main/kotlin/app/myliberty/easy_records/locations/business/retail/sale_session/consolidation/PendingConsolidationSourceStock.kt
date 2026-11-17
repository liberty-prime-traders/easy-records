package app.myliberty.easy_records.locations.business.sale_session.consolidation

import app.myliberty.easy_records.locations.business.sale.api.SaleHeaderDto
import app.myliberty.easy_records.locations.business.sale.api.SaleLineDto
import app.myliberty.easy_records.locations.business.sale.api.SaleStatus
import app.myliberty.easy_records.locations.business.sale_session.api.StockHeldBySales
import app.myliberty.easy_records.util.quantity.ExactQuantity
import java.math.BigDecimal
import java.util.UUID

object PendingConsolidationSourceStock {

    fun calculate(
        sourceSaleHeaders: List<SaleHeaderDto>,
        sourceSaleLinesBySaleId: Map<UUID, List<SaleLineDto>>,
    ): Map<UUID, StockHeldBySales> =
        sourceSaleHeaders
            .flatMap { sourceSaleHeader ->
                sourceSaleLinesBySaleId.getValue(sourceSaleHeader.id).map { sourceSaleLine ->
                    sourceSaleLine.locationProductId to heldBy(sourceSaleHeader.status, sourceSaleLine)
                }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, stockHeldPerLine) ->
                stockHeldPerLine.fold(StockHeldBySales.NONE) { total, next -> total + next }
            }

    private fun heldBy(sourceSaleStatus: SaleStatus, sourceSaleLine: SaleLineDto): StockHeldBySales {
        val storageQuantity: BigDecimal = ExactQuantity.ofStorageFromUserProvided(
            sourceSaleLine.userProvidedQuantity, sourceSaleLine.storageUnitsPerUserProvidedUnit
        ).toStorageQuantity()
        return when (sourceSaleStatus) {
            SaleStatus.DRAFT -> StockHeldBySales(
                storageQuantityReserved = storageQuantity, storageQuantityTaken = BigDecimal.ZERO
            )
            SaleStatus.CONFIRMED -> StockHeldBySales(
                storageQuantityReserved = BigDecimal.ZERO, storageQuantityTaken = storageQuantity
            )
            SaleStatus.DISCARDED,
            SaleStatus.VOIDED -> StockHeldBySales.NONE
        }
    }
}
