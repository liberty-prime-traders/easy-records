package app.myliberty.easy_records.locations.business.retail.stock_adjustment.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.StockAdjustmentCostRepository
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.StockAdjustmentEntity
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.search.StockAdjustmentSearchFetcher
import app.myliberty.easy_records.util.business.mappers.UserQualifier
import app.myliberty.easy_records.util.paging.PageResponse
import app.myliberty.easy_records.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnLocationSchema(readOnly = true)
class StockAdjustmentDataFetcher(
    private val stockAdjustmentCostRepository: StockAdjustmentCostRepository,
    private val stockAdjustmentSearchFetcher: StockAdjustmentSearchFetcher,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val userQualifier: UserQualifier
) {

    fun search(
        stockAdjustmentSearchParameters: StockAdjustmentSearchParameters,
        previousCursor: String,
        requestedSize: Int
    ): PageResponse<StockAdjustmentResponseDto, String> {
        val cursor = KeysetSearchCursor.decode(previousCursor)
        val stockAdjustments = stockAdjustmentSearchFetcher.search(
            stockAdjustmentSearchParameters, cursor, requestedSize
        )
        val hasMore = stockAdjustments.size > requestedSize
        val pageRows = if (hasMore) stockAdjustments.take(requestedSize) else stockAdjustments

        val currentCursor = pageRows.lastOrNull()
            ?.let { stockAdjustment ->
                KeysetSearchCursor(stockAdjustment.requiredCreatedOn(), stockAdjustment.id!!).encode()
            }
            ?: previousCursor

        return PageResponse(currentCursor = currentCursor, hasMore = hasMore, contents = buildResponses(pageRows))
    }

    fun buildResponse(
        stockAdjustmentEntity: StockAdjustmentEntity,
        productLabel: String,
        activeTotalCost: BigDecimal
    ) = StockAdjustmentResponseDto(
        referenceNumber = stockAdjustmentEntity.requiredReference(),
        locationProductId = stockAdjustmentEntity.locationProductId,
        productLabel = productLabel,
        category = stockAdjustmentEntity.category,
        direction = stockAdjustmentEntity.direction,
        userProvidedQuantity = stockAdjustmentEntity.userProvidedQuantity,
        unitId = stockAdjustmentEntity.unitId,
        quantityText = stockAdjustmentEntity.quantityText,
        totalCost = activeTotalCost,
        stockMovementReasonId = stockAdjustmentEntity.stockMovementReasonId,
        note = stockAdjustmentEntity.note,
        adjustedBy = userQualifier.getCreatorFullName(stockAdjustmentEntity.requiredCreatedById()),
        adjustedAt = stockAdjustmentEntity.requiredCreatedOn()
    )

    private fun buildResponses(stockAdjustments: List<StockAdjustmentEntity>): List<StockAdjustmentResponseDto> {
        val activeTotalCostByStockAdjustmentId = stockAdjustmentCostRepository
            .findByStockAdjustmentIdInAndStatus(stockAdjustments.map { it.id!! }, StockAdjustmentCostStatus.ACTIVE)
            .associate { it.stockAdjustmentId to it.totalCost }
        val labelsByLocationProductId = locationProductDataFetcher
            .findSummaryByIds(stockAdjustments.map { stockAdjustment -> stockAdjustment.locationProductId }.toSet())
            .mapValues { locationProductSummary -> locationProductSummary.value.printLabel }
        return stockAdjustments.map { stockAdjustment ->
            buildResponse(
                stockAdjustment,
                labelsByLocationProductId.getValue(stockAdjustment.locationProductId),
                activeTotalCostByStockAdjustmentId.getValue(stockAdjustment.id!!)
            )
        }
    }
}
