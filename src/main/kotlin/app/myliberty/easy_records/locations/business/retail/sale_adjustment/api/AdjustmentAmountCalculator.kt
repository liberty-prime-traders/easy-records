package app.myliberty.easy_records.locations.business.retail.sale_adjustment.api

import app.myliberty.easy_records.locations.business.retail.location_product.api.LinePricing
import app.myliberty.easy_records.locations.business.retail.location_product.api.ProductLineWithPrice
import app.myliberty.easy_records.util.business.Decimals
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import java.math.BigDecimal

object AdjustmentAmountCalculator {

    fun calculateAmount(saleAdjustmentCreateDto: SaleAdjustmentCreateDto, productLinesWithPrice: List<ProductLineWithPrice>): BigDecimal =
        when (saleAdjustmentCreateDto.calculationMethod) {
            PriceAdjustmentMethod.UNIT_PRICE_OVERRIDE -> {
                val overriddenLine = adjustedLine(saleAdjustmentCreateDto, productLinesWithPrice)
                Decimals.MoneyScale.round(
                    LinePricing.overrideDifference(overriddenLine.lineTotal, saleAdjustmentCreateDto.value, overriddenLine.userProvidedQuantity).abs()
                )
            }
            PriceAdjustmentMethod.PERCENTAGE -> {
                val basePrice = if (saleAdjustmentCreateDto.saleLineKey != null) {
                    adjustedLine(saleAdjustmentCreateDto, productLinesWithPrice).lineTotal
                } else {
                    productLinesWithPrice.sumOf { it.lineTotal }
                }
                Decimals.MoneyScale.divide(saleAdjustmentCreateDto.value.multiply(basePrice), BigDecimal(100))
            }
        }

    private fun adjustedLine(
        saleAdjustmentCreateDto: SaleAdjustmentCreateDto,
        productLinesWithPrice: List<ProductLineWithPrice>,
    ): ProductLineWithPrice = productLinesWithPrice.singleOrNull { it.lineKey == saleAdjustmentCreateDto.saleLineKey }
        ?: throw RtsGenericException("Adjustment references a line that is not on the sale")
}
