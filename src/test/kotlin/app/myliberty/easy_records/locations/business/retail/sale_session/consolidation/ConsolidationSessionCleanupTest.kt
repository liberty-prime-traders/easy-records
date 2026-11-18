package app.myliberty.easy_records.locations.business.retail.sale_session.consolidation

import app.myliberty.easy_records.configuration.session.LocationSession
import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionBuilder
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionStore
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSession
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionHeader
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.UUID

class ConsolidationSessionCleanupTest {

    private val saleSessionStore = mock(SaleSessionStore::class.java)
    private val consolidationSessionCleanup = ConsolidationSessionCleanup(saleSessionStore)
    private val pendingSessionId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SessionContextProvider.setSession(
            SessionContext(
                systemUserId = UUID.randomUUID(),
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = UUID.randomUUID(), schemaName = "loc-1"),
            )
        )
        TransactionSynchronizationManager.initSynchronization()
    }

    @AfterEach
    fun clearTransactionSynchronization() {
        TransactionSynchronizationManager.clearSynchronization()
    }

    private fun newSession(): SaleSession = SaleSessionBuilder(UUID.randomUUID())
        .withHeader(SaleSessionHeader(null, UUID.randomUUID(), null, null, null))
        .build()

    private fun commit() = TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }

    @Test
    fun `nothing is touched before the commit, and the replaced sessions go after it`() {
        val newSession = newSession()
        consolidationSessionCleanup.replaceAfterCommit(newSession, emptyList(), pendingSessionId)

        verifyNoInteractions(saleSessionStore)
        commit()

        verify(saleSessionStore).save(newSession)
        verify(saleSessionStore).delete(pendingSessionId)
    }

    @Test
    fun `a failed save after the commit says the sales were consolidated and where to find the draft`() {
        val newSession = newSession()
        doThrow(IllegalStateException("redis down")).`when`(saleSessionStore).save(newSession)
        consolidationSessionCleanup.replaceAfterCommit(newSession, emptyList(), pendingSessionId)

        val refusal = assertThrows(LibertyGenericException::class.java) { commit() }

        assertTrue(refusal.message.contains("The sales were consolidated"))
        assertTrue(refusal.message.contains("sales grid"))
        verify(saleSessionStore).delete(pendingSessionId)
    }

    @Test
    fun `a failed delete of a replaced session is only logged`() {
        val newSession = newSession()
        doThrow(IllegalStateException("redis down")).`when`(saleSessionStore).delete(pendingSessionId)
        consolidationSessionCleanup.replaceAfterCommit(newSession, emptyList(), pendingSessionId)

        commit()

        verify(saleSessionStore).save(newSession)
    }
}
