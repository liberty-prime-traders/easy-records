package app.myliberty.easy_records.locations.business.retail.sale_session

import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSession
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
@Profile("no-redis")
class SaleSessionInMemoryStore : SaleSessionStore {

    private val sessions = ConcurrentHashMap<UUID, SaleSession>()
    private val bySale = ConcurrentHashMap<UUID, UUID>()

    override fun save(session: SaleSession) {
        sessions[session.sessionId] = session
        session.saleId?.let { bySale[it] = session.sessionId }
    }

    override fun load(sessionId: UUID): SaleSession =
        loadOrNull(sessionId) ?: throw RtsGenericException("Sale session $sessionId not found or expired")

    override fun loadOrNull(sessionId: UUID): SaleSession? = sessions[sessionId]

    override fun delete(sessionId: UUID) {
        val removed = sessions.remove(sessionId)
        removed?.saleId?.let { bySale.remove(it) }
    }

    override fun findOpenSessionForSale(saleId: UUID): SaleSession? {
        val sessionId = bySale[saleId] ?: return null
        return sessions[sessionId]
    }

    override fun listOpenSessions(): List<SaleSession> = sessions.values.toList()
}
