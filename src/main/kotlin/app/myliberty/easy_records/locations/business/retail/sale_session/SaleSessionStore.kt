package app.myliberty.easy_records.locations.business.retail.sale_session

import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSession
import java.util.UUID

interface SaleSessionStore {

    fun save(session: SaleSession)

    fun load(sessionId: UUID): SaleSession

    fun loadOrNull(sessionId: UUID): SaleSession?

    fun delete(sessionId: UUID)

    fun findOpenSessionForSale(saleId: UUID): SaleSession?

    fun listOpenSessions(): List<SaleSession>
}
