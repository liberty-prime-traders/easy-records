package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionHeader
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SaleSessionLoader(
    private val saleDataFetcher: SaleDataFetcher,
    private val saleAdjustmentFetcher: SaleAdjustmentFetcher,
    private val salePaymentFetcher: SalePaymentFetcher,
    private val domainToSessionMapper: DomainToSessionMapper,
    private val saleSessionStockOverlay: SaleSessionStockOverlay,
) {

    fun newSession(sessionId: UUID, contactId: UUID): SaleSession {
        val emptyHeader = SaleSessionHeader(
            contactId = contactId,
            soldById = null,
            dateSold = null,
            notes = null,
            referenceNumber = null
        )
        return finish(SaleSessionBuilder(sessionId).withHeader(emptyHeader))
    }

    fun loadFromSale(sessionId: UUID, saleId: UUID): SaleSession {
        val saleHeader = saleDataFetcher.getSaleHeader(saleId)
        val saleLineSnapshots = saleDataFetcher.getSaleLines(saleId)
        val saleAdjustmentSnapshots = saleAdjustmentFetcher.getAdjustments(saleId)
        val salePaymentSnapshots = salePaymentFetcher.getPaymentSnapshots(saleId)

        val saleSessionLines = saleLineSnapshots.map(domainToSessionMapper::toSaleSessionLine)
        val relatedSaleLineIdentityBySaleLineId = saleSessionLines.associate { it.identity.id!! to it.identity }
        val saleSessionAdjustments = saleAdjustmentSnapshots.map { adjustmentSnapshot ->
            domainToSessionMapper.toSaleSessionAdjustment(adjustmentSnapshot, relatedSaleLineIdentityBySaleLineId)
        }
        val saleSessionPayments = salePaymentSnapshots.map(domainToSessionMapper::toSaleSessionPayment)

        return finish(
            SaleSessionBuilder(sessionId)
                .linkedToPersistedSale(saleHeader.id, saleHeader.version, saleHeader.status)
                .withHeader(domainToSessionMapper.toSaleSessionHeader(saleHeader))
                .withSaleLines(saleSessionLines)
                .withSaleAdjustments(saleSessionAdjustments)
                .withSalePayments(saleSessionPayments)
        )
    }

    private fun finish(saleSessionBuilder: SaleSessionBuilder): SaleSession =
        SaleSessionTotalsCalculator.recompute(saleSessionStockOverlay.populate(saleSessionBuilder.build()))
}
