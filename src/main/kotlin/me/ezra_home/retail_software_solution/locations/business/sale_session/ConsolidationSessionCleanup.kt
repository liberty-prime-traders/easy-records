package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleHeaderDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.UUID

@Component
class ConsolidationSessionCleanup(private val saleSessionStore: SaleSessionStore) {

    // Redis is not part of the database transaction; saving or deleting before the commit could outlive a rollback.
    fun replaceAfterCommit(newSession: SaleSession, sourceSaleHeaders: List<SaleHeaderDto>, pendingSessionId: UUID?) {
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                saleSessionStore.save(newSession)
                sourceSaleHeaders.forEach { sourceSaleHeader ->
                    saleSessionStore.findOpenSessionForSale(sourceSaleHeader.id)
                        ?.let { saleSessionStore.delete(it.sessionId) }
                }
                pendingSessionId?.let { saleSessionStore.delete(it) }
            }
        })
    }
}
