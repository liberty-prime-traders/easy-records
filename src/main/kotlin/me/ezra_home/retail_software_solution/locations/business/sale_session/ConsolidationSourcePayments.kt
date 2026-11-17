package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleHeaderDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleUpdater
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentService
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentSnapshot
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentVoidCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionPayment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import org.springframework.stereotype.Component

@Component
class ConsolidationSourcePayments(
    private val salePaymentFetcher: SalePaymentFetcher,
    private val salePaymentService: SalePaymentService,
    private val domainToSessionMapper: DomainToSessionMapper,
) {

    fun activePaymentsOf(sourceSaleHeaders: List<SaleHeaderDto>): List<SalePaymentSnapshot> =
        salePaymentFetcher.getPaymentSnapshotsBySaleId(sourceSaleHeaders.map { it.id })
            .values
            .flatten()
            .filter { it.voidedReason == null }

    // No payment date: the writer dates it when the draft is saved, so it never lands in a past fiscal period.
    fun asNewSessionPayments(activePayments: List<SalePaymentSnapshot>): List<SaleSessionPayment> =
        activePayments.map { salePaymentSnapshot ->
            domainToSessionMapper.toSaleSessionPayment(salePaymentSnapshot)
                .copy(identity = SessionIdentity.mintFreshIdentity(), paymentDate = null)
        }

    fun voidAll(activePayments: List<SalePaymentSnapshot>, consolidatedReferenceNumber: String) {
        if (activePayments.isEmpty()) return
        val voidReason = SaleUpdater.consolidationVoidReason(consolidatedReferenceNumber)
        salePaymentService.voidPayments(activePayments.map { SalePaymentVoidCreateDto(it.id, voidReason) })
    }
}
