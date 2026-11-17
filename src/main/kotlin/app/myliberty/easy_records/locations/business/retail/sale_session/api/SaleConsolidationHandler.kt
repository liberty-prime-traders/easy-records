package app.myliberty.easy_records.locations.business.retail.sale_session.api

import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.PendingConsolidationPromoter
import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.ConsolidationGenerateInput
import app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.ReviewedProductLines
import app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.SaleConsolidationGenerator
import app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.SaleConsolidationStarter
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionStore
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionValidator
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class SaleConsolidationHandler(
    private val saleConsolidationStarter: SaleConsolidationStarter,
    private val saleConsolidationGenerator: SaleConsolidationGenerator,
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionValidator: SaleSessionValidator,
    private val pendingConsolidationPromoter: PendingConsolidationPromoter,
) {

    fun consolidate(saleConsolidationRequestDto: SaleConsolidationRequestDto): SaleSessionResponseDto =
        saleConsolidationStarter.start(saleConsolidationRequestDto.saleIds)

    fun generate(sessionId: UUID): SaleSessionResponseDto {
        val loadedSession = saleSessionStore.load(sessionId)
        val pendingSession = pendingConsolidationPromoter.promote(loadedSession)
        saleSessionValidator.guardAllowed(pendingSession, SaleSessionAction.GENERATE_CONSOLIDATED_SALE)
        if (pendingSession !== loadedSession) {
            saleSessionStore.save(pendingSession)
            throw LibertyGenericException("Some products still cannot be combined. Reload this page to see them")
        }
        return saleConsolidationGenerator.generate(
            ConsolidationGenerateInput(
                sourceSales = pendingSession.requiredPendingConsolidation().sourceSales,
                reviewedProductLines = ReviewedProductLines.of(pendingSession),
                pendingSessionId = sessionId,
            )
        )
    }
}
