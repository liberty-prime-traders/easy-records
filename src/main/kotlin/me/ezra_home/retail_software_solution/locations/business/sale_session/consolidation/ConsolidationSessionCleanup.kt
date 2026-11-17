package me.ezra_home.retail_software_solution.locations.business.sale_session.consolidation

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleHeaderDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStore
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.UUID

@Component
class ConsolidationSessionCleanup(private val saleSessionStore: SaleSessionStore) {

    private val log = LoggerFactory.getLogger(javaClass)

    // Redis is not part of the database transaction; saving or deleting before the commit could outlive a rollback.
    fun replaceAfterCommit(newSession: SaleSession, sourceSaleHeaders: List<SaleHeaderDto>, pendingSessionId: UUID?) {
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                deleteReplacedSessions(sourceSaleHeaders, pendingSessionId)
                saveNewSession(newSession)
            }
        })
    }

    // A session that is not deleted expires on its own, so a failure here is only logged.
    private fun deleteReplacedSessions(sourceSaleHeaders: List<SaleHeaderDto>, pendingSessionId: UUID?) {
        try {
            sourceSaleHeaders.forEach { sourceSaleHeader ->
                saleSessionStore.findOpenSessionForSale(sourceSaleHeader.id)
                    ?.let { saleSessionStore.delete(it.sessionId) }
            }
            pendingSessionId?.let { saleSessionStore.delete(it) }
        } catch (exception: Exception) {
            log.warn("Could not delete the sessions replaced by a consolidation", exception)
        }
    }

    // The sales are already consolidated here, so the message says the draft exists and where to find it.
    private fun saveNewSession(newSession: SaleSession) {
        try {
            saleSessionStore.save(newSession)
        } catch (exception: Exception) {
            log.error("Could not save the session of consolidated sale ${newSession.saleId}", exception)
            throw RtsGenericException(
                "The sales were consolidated, but something went wrong opening the new draft. " +
                    "Refresh the page and open the new draft from the sales grid."
            )
        }
    }
}
