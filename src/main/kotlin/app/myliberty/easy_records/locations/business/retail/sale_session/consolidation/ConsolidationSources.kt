package app.myliberty.easy_records.locations.business.retail.sale_session.consolidation

import app.myliberty.easy_records.locations.business.retail.sale.api.SaleDataFetcher
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleHeaderDto
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionValidator
import app.myliberty.easy_records.locations.business.retail.sale_session.api.ConsolidationSourceSale
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ConsolidationSources(
    private val saleDataFetcher: SaleDataFetcher,
    private val saleSessionValidator: SaleSessionValidator,
) {

    // The locks come first: a header read before them would come from the persistence context and hide a later change.
    fun lock(saleIds: List<UUID>): List<SaleHeaderDto> {
        saleSessionValidator.guardConsolidationSelectionSize(saleIds.size)
        return saleDataFetcher.lockAndGetSaleHeaders(saleIds)
    }

    fun guardSelectable(sourceSaleHeaders: List<SaleHeaderDto>) {
        saleSessionValidator.guardConsolidationSelectionSize(sourceSaleHeaders.size)
        saleSessionValidator.guardSingleContactAcrossSales(sourceSaleHeaders)
        saleSessionValidator.guardSourceSalesConsolidatable(sourceSaleHeaders)
    }

    fun guardUnchangedSinceSelection(
        lockedHeaders: List<SaleHeaderDto>,
        sourceSales: List<ConsolidationSourceSale>,
    ) {
        val lockedHeaderBySaleId = lockedHeaders.associateBy { it.id }
        sourceSales.forEach { sourceSale ->
            if (lockedHeaderBySaleId.getValue(sourceSale.saleId).version != sourceSale.saleVersion) {
                throw LibertyGenericException(
                    "Sale ${sourceSale.referenceNumber} changed after it was selected. Select the sales again"
                )
            }
        }
    }
}
