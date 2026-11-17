package app.myliberty.easy_records.locations.business.sale_session.api

import java.util.UUID

data class PendingConsolidation(
    val sourceSales: List<ConsolidationSourceSale>,
    // Kept for removed lines too, so a restored line still names its source sale.
    val sourceSaleReferenceByLineKey: Map<UUID, String>,
    val removedSaleLines: List<RemovedSaleLine> = emptyList(),
    // A snapshot from selection: what the source sales hold per product, which Generate frees by voiding them.
    val stockHeldBySourceSalesByLocationProductId: Map<UUID, StockHeldBySales> = emptyMap(),
    // Products that combined cleanly at selection and are not held on the form.
    val cleanLocationProductIds: Set<UUID> = emptySet(),
    // Sources with a sale-wide adjustment; a pending consolidation does not carry it onto the new sale.
    val saleReferencesWithOrderLevelAdjustments: Set<String> = emptySet(),
) {

    fun removedSaleLineResponses(): List<RemovedSaleLineResponse> = removedSaleLines.map { removedSaleLine ->
        val saleLine = removedSaleLine.saleLine
        RemovedSaleLineResponse(
            identity = saleLine.identity,
            locationProductId = saleLine.locationProductId,
            productLabel = saleLine.productLabel,
            userProvidedQuantity = saleLine.userProvidedQuantity,
            unitId = saleLine.unitId,
            unitPrice = saleLine.defaultSalePrice,
            lineTotal = saleLine.lineTotal,
            sourceSaleReference = sourceSaleReferenceByLineKey.getValue(saleLine.lineKey),
        )
    }
}
