package app.myliberty.easy_records.locations.business.retail.stock.api.revision

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.stock.StockEntryRepository
import app.myliberty.easy_records.locations.business.retail.stock.StockIncreaseRecorder
import app.myliberty.easy_records.locations.business.retail.stock.StockReplacementLayerSpecs
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID
import org.springframework.transaction.annotation.Propagation

@Service
@TransactionalOnLocationSchema
class StockReplacementLayerRecorder(
    private val stockIncreaseRecorder: StockIncreaseRecorder,
    private val stockEntryRepository: StockEntryRepository,
) {

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun record(stockReplacementLayerRequest: StockReplacementLayerRequest): UUID {
        val revisedStockEntry = stockEntryRepository.findById(
            stockReplacementLayerRequest.revisedStockEntryId
        ).orElseThrow {
            RtsGenericException(
                "The stock of this delivery could not be found. Please contact support."
            )
        }
        return stockIncreaseRecorder.record(
            StockReplacementLayerSpecs.of(stockReplacementLayerRequest, revisedStockEntry)
        )
    }
}
