package app.myliberty.easy_records.locations.business

import app.myliberty.easy_records.support.WithDefaultOrganizationSession
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import app.myliberty.easy_records.locations.business.retail.delivery.api.PurchaseDeliveryLineCreateDto
import app.myliberty.easy_records.locations.business.retail.opening_stock.api.OpeningStockLineDto
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseLineCreateDto
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseLineUpdateDto
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.api.StockAdjustmentCreateDto
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

@WithDefaultOrganizationSession
class RequestUnitCostPrecisionTest {

    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()
    private val id = UUID.randomUUID()

    private fun everyRequestBuiltWith(unitCost: String): List<() -> Any> = listOf(
        { read<PurchaseLineCreateDto>("""{"locationProductId":"$id","quantityOrdered":1,"unitId":"$id","unitCost":$unitCost}""") },
        { read<PurchaseLineUpdateDto>("""{"id":"$id","quantityOrdered":1,"unitId":"$id","unitCost":$unitCost}""") },
        { read<PurchaseDeliveryLineCreateDto>("""{"purchaseLineId":"$id","quantityDelivered":1,"unitId":"$id","unitCost":$unitCost}""") },
        { read<OpeningStockLineDto>("""{"locationProductId":"$id","quantity":1,"unitId":"$id","unitCost":$unitCost}""") },
        { read<StockAdjustmentCreateDto>("""{"locationProductId":"$id","category":"OTHER","quantity":1,"unitId":"$id","unitCost":$unitCost}""") },
    )

    private inline fun <reified RequestType> read(json: String): RequestType = objectMapper.readValue(json, RequestType::class.java)

    @Test
    fun `a typed unit cost with three decimals is a business error in every request that carries one`() {
        everyRequestBuiltWith("1.255").forEach { build ->
            val thrown = assertThrows(Exception::class.java) { build() }
            assertTrue(generateSequence<Throwable>(thrown) { it.cause }.any { it is RtsGenericException })
        }
    }

    @Test
    fun `a unit cost written with trailing zeros beyond two decimals is accepted everywhere`() {
        everyRequestBuiltWith("1.2500").forEach { build -> build() }
    }
}
