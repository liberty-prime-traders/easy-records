package app.myliberty.easy_records.locations.business.retail.sale

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductSummaryDto
import app.myliberty.easy_records.locations.business.retail.location_product.api.ProductStockBreakdownProvider
import app.myliberty.easy_records.locations.business.core.lock.api.EntityAdvisoryLock
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleSaveRequest
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleStatus
import app.myliberty.easy_records.locations.business.retail.sale_payment.api.SalePaymentFetcher
import app.myliberty.easy_records.locations.business.retail.stock.api.read.StockBalanceFetcher
import app.myliberty.easy_records.locations.business.retail.stock_reservation.api.StockReserver
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import app.myliberty.easy_records.util.quantity.ExactQuantity
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class SaleValidator(
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val salePaymentFetcher: SalePaymentFetcher,
    private val stockReserver: StockReserver,
    private val productStockBreakdownProvider: ProductStockBreakdownProvider,
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    companion object {
        fun guardNoPreAllocatedReferenceOnExistingSale(saleSaveRequest: SaleSaveRequest) {
            if (saleSaveRequest.preAllocatedReferenceNumber != null) {
                throw RtsGenericException("A reference number can only be pre-allocated for a new sale")
            }
        }

        fun guardNoPreAllocatedReferenceOnConfirm(saleSaveRequest: SaleSaveRequest) {
            if (saleSaveRequest.preAllocatedReferenceNumber != null) {
                throw RtsGenericException("A reference number can only be pre-allocated when saving a draft")
            }
        }

        fun guardDateSoldIsNotFuture(dateSold: OffsetDateTime) {
            val orgToday = DateTimes.Local.Now.organization()
            val saleLocalDate = DateTimes.Local.atOrganizationZone(dateSold)
            if (saleLocalDate > orgToday) {
                throw RtsGenericException("Sale date must be today or in the past. Provided date: $saleLocalDate")
            }
        }
    }

    fun guardCanVoid(sales: List<SaleEntity>) {
        val saleIdsWithActivePayments = salePaymentFetcher.saleIdsWithActivePayments(sales.map { it.id!! })
        sales.forEach { guardCanVoid(it, it.id!! in saleIdsWithActivePayments) }
    }

    private fun guardCanVoid(sale: SaleEntity, hasActivePayments: Boolean) {
        if (sale.status == SaleStatus.VOIDED) {
            throw RtsGenericException("Sale ${sale.referenceNumber} is already voided")
        }
        if (sale.status == SaleStatus.DISCARDED) {
            throw RtsGenericException("Sale ${sale.referenceNumber} is discarded and cannot be voided")
        }
        if (hasActivePayments) {
            throw RtsGenericException("Cannot void a sale with active payments")
        }
        if (sale.status != SaleStatus.DRAFT && sale.taxTotal == null) {
            throw RtsGenericException("Cannot void sale ${sale.referenceNumber} — tax finalization is still pending")
        }
    }

    fun guardSufficientStockForSale(
        saleId: UUID,
        requested: Map<UUID, ExactQuantity>,
        productSummaries: Map<UUID, LocationProductSummaryDto>
    ) {
        if (requested.isEmpty()) return
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, requested.keys)
        val balances = stockBalanceFetcher.getLatestBalances(requested.keys.toList())
        val reservations = stockReserver.loadReservationBreakdown(requested.keys)
        val breakdownByLocationProductId = productStockBreakdownProvider.forLocationProducts(requested.keys)
        requested.forEach { (locationProductId, storageQuantity) ->
            val reservedByOthers = reservations[locationProductId]?.excludingSale(saleId) ?: BigDecimal.ZERO
            val available = ExactQuantity.ofStorage(balances.getValue(locationProductId)) - ExactQuantity.ofStorage(reservedByOthers)
            if (available < storageQuantity) {
                val label = productSummaries[locationProductId]?.label ?: locationProductId.toString()
                val quantityBreakdown = breakdownByLocationProductId.getValue(locationProductId)
                throw RtsGenericException(
                    "Insufficient stock for $label. Available: ${quantityBreakdown.render(available)}, Requested: ${quantityBreakdown.render(storageQuantity)}"
                )
            }
        }
    }

}
