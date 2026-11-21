package app.myliberty.easy_records.locations.business

import app.myliberty.easy_records.support.WithDefaultOrganizationSession
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import app.myliberty.easy_records.locations.business.retail.delivery.api.PurchaseDeliveryLineCreateDto
import app.myliberty.easy_records.locations.business.retail.opening_stock.api.OpeningStockLineDto
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseCancelLinesDto
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseLineCreateDto
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseLineUpdateDto
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionLineAddDto
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionLineUpdateDto
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.api.StockAdjustmentCreateDto
import app.myliberty.easy_records.locations.business.retail.stock_transfer.api.StockTransferLineInsertDto
import app.myliberty.easy_records.locations.business.retail.stock_transfer.api.StockTransferLineUpdateDto
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

@WithDefaultOrganizationSession
class RequestQuantityPrecisionTest {

    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()
    private val id = UUID.randomUUID()

    private fun everyRequestBuiltWith(quantity: String): List<() -> Any> = listOf(
        { read<SaleSessionLineAddDto>("""{"locationProductId":"$id","quantity":$quantity}""") },
        {
            read<SaleSessionLineUpdateDto>(
                """{"identity":{"transientId":"$id"},"quantity":$quantity,"unitId":"$id","unitPriceOverride":1.25}"""
            )
        },
        { read<PurchaseLineCreateDto>("""{"locationProductId":"$id","quantityOrdered":$quantity,"unitId":"$id","unitCost":1.00}""") },
        { read<PurchaseLineUpdateDto>("""{"id":"$id","quantityOrdered":$quantity,"unitId":"$id","unitCost":1.00}""") },
        { read<PurchaseCancelLinesDto>("""{"purchaseLineId":"$id","quantityCanceled":$quantity}""") },
        { read<PurchaseDeliveryLineCreateDto>("""{"purchaseLineId":"$id","quantityDelivered":$quantity,"unitId":"$id","unitCost":1.00}""") },
        { read<OpeningStockLineDto>("""{"locationProductId":"$id","quantity":$quantity,"unitId":"$id","unitCost":1.00}""") },
        {
            read<StockAdjustmentCreateDto>(
                """{"locationProductId":"$id","category":"OTHER","quantity":$quantity,"unitId":"$id"}"""
            )
        },
        { read<StockTransferLineInsertDto>("""{"locationProductId":"$id","quantityDispatched":$quantity}""") },
        { read<StockTransferLineUpdateDto>("""{"lineRef":"TRL1","quantityDispatched":$quantity}""") },
    )

    private inline fun <reified RequestType> read(json: String): RequestType = objectMapper.readValue(json, RequestType::class.java)

    @Test
    fun `a typed quantity with three decimals is a business error in every request that carries one`() {
        everyRequestBuiltWith("1.255").forEach { build ->
            val thrown = assertThrows(Exception::class.java) { build() }
            assertTrue(generateSequence<Throwable>(thrown) { it.cause }.any { it is LibertyGenericException })
        }
    }

    @Test
    fun `a quantity written with trailing zeros beyond two decimals is accepted everywhere`() {
        everyRequestBuiltWith("1.2500").forEach { build -> build() }
    }
}
