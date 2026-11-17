package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.locations.business.sale_session.consolidation.PendingConsolidationPromoter
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale_session.consolidation.ConsolidationGenerateInput
import me.ezra_home.retail_software_solution.locations.business.sale_session.consolidation.ReviewedProductLines
import me.ezra_home.retail_software_solution.locations.business.sale_session.consolidation.SaleConsolidationGenerator
import me.ezra_home.retail_software_solution.locations.business.sale_session.consolidation.SaleConsolidationStarter
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStore
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionValidator
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
            throw RtsGenericException("Some products still cannot be combined. Reload this page to see them")
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
