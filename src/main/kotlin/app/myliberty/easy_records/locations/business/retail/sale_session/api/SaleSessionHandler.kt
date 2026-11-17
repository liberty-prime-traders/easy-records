package app.myliberty.easy_records.locations.business.sale_session.api

import app.myliberty.easy_records.locations.business.sale_session.consolidation.PendingConsolidationPromoter
import com.google.common.base.Predicate
import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.sale_session.SaleSessionAssembler
import app.myliberty.easy_records.locations.business.sale_session.SaleSessionCopier
import app.myliberty.easy_records.locations.business.sale_session.SaleSessionLoader
import app.myliberty.easy_records.locations.business.sale_session.SaleSessionStore
import app.myliberty.easy_records.locations.business.sale_session.SaleSessionUpdateFinalizer
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaleSessionHandler(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionLoader: SaleSessionLoader,
    private val saleSessionCopier: SaleSessionCopier,
    private val saleSessionAssembler: SaleSessionAssembler,
    private val saleSessionUpdateFinalizer: SaleSessionUpdateFinalizer,
    private val pendingConsolidationPromoter: PendingConsolidationPromoter,
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun start(sessionStartDto: SaleSessionStartDto): SaleSessionResponseDto {
        sessionStartDto.copyFromSaleId?.let { sourceSaleId ->
            val copiedSession = saleSessionCopier.copyFromSale(UUID.randomUUID(), sourceSaleId)
            return saleSessionUpdateFinalizer.finalize(copiedSession)
        }
        sessionStartDto.saleId?.let { saleId ->
            saleSessionStore.findOpenSessionForSale(saleId)?.let { existingSession ->
                return saleSessionAssembler.buildResponse(existingSession)
            }
        }
        val sessionId = UUID.randomUUID()
        val session = if (sessionStartDto.saleId != null) {
            saleSessionLoader.loadFromSale(sessionId, sessionStartDto.saleId)
        } else {
            saleSessionLoader.newSession(sessionId, sessionStartDto.contactId!!)
        }
        return saleSessionUpdateFinalizer.finalize(session)
    }

    fun abandon(sessionId: UUID) {
        saleSessionStore.delete(sessionId)
    }

    fun getSessionsForUnsavedSales(mineOnly: Boolean): List<SaleSessionSummaryDto> {
        val predicate: Predicate<SaleSession> = Predicate { saleSession ->
            saleSession.saleId == null && (!mineOnly || saleSession.createdById == SessionContextProvider.getUserId())
        }
        return saleSessionStore.listOpenSessions()
            .filter(predicate::apply)
            .let { saleSessionAssembler.buildSummaries(it) }
    }

    @TransactionalOnLocationSchema(readOnly = true)
    fun acquireSession(sessionId: UUID): SaleSessionResponseDto {
        val session = pendingConsolidationPromoter.promote(saleSessionStore.load(sessionId))
        val visitedSession = session.markVisited(SessionContextProvider.getUserId())
        saleSessionStore.save(visitedSession)
        return saleSessionAssembler.buildResponse(visitedSession)
    }
}
