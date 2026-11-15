package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import java.util.UUID

data class PendingConsolidation(
    val sourceSales: List<ConsolidationSourceSale>,
    // Kept for removed lines too, so a restored line still names its source sale.
    val sourceSaleReferenceByLineKey: Map<UUID, String>,
    val removedSaleLines: List<RemovedSaleLine> = emptyList(),
    // A snapshot from selection: what the source sales hold per product, which Generate frees by voiding them.
    val stockHeldBySourceSalesByLocationProductId: Map<UUID, StockHeldBySales> = emptyMap(),
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
