package app.myliberty.easy_records.locations.business.retail.stock_adjustment.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.StockAdjustmentCategoryRules
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.StockAdjustmentRepository
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema(readOnly = true)
class StockAdjustmentAccountCodesFetcher(
    private val stockAdjustmentRepository: StockAdjustmentRepository,
) {

    fun getByReferenceNumber(referenceNumbers: Collection<String>): Map<String, StockAdjustmentAccountCodes> =
        stockAdjustmentRepository.findByReferenceNumberIn(referenceNumbers).associate { stockAdjustment ->
            val ledgerAccounts = StockAdjustmentCategoryRules.ledgerAccounts(
                stockAdjustment.category, stockAdjustment.direction
            )
            stockAdjustment.requiredReference() to
                StockAdjustmentAccountCodes(ledgerAccounts.debit.code, ledgerAccounts.credit.code)
        }
}
