package app.myliberty.easy_records.locations.business.stock_revision

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.delivery.api.PurchaseDeliveryLineReplacer
import app.myliberty.easy_records.locations.business.purchase.api.delivery.PurchaseDeliveryRevisionApplier
import app.myliberty.easy_records.locations.business.stock.api.revision.StockReplacementLayerRecorder
import app.myliberty.easy_records.locations.business.stock.api.revision.StockRevisionCommitter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Propagation

class RevisionWritePathPropagationTest {

    private fun propagationOf(serviceClass: Class<*>, methodName: String): Propagation =
        serviceClass.declaredMethods.single { it.name == methodName }
            .getAnnotation(TransactionalOnLocationSchema::class.java)
            .propagation

    @Test
    fun `every write of a revision joins the revisioner's transaction and cannot run alone`() {
        val mandatoryWrites = mapOf(
            StockRevisionCommitter::class.java to listOf("commitReset", "commitReplay"),
            StockReplacementLayerRecorder::class.java to listOf("record"),
            PurchaseDeliveryLineReplacer::class.java to listOf("replace"),
            PurchaseDeliveryRevisionApplier::class.java to listOf("apply"),
        )

        mandatoryWrites.forEach { (serviceClass, methodNames) ->
            methodNames.forEach { methodName ->
                assertEquals(
                    Propagation.MANDATORY, propagationOf(serviceClass, methodName), "$serviceClass.$methodName"
                )
            }
        }
    }
}
