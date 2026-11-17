package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale_session.ConsolidationGenerateInput
import me.ezra_home.retail_software_solution.locations.business.sale_session.ReviewedProductLines
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleConsolidationGenerator
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleConsolidationStarter
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
) {

    fun select(saleConsolidationSelectDto: SaleConsolidationSelectDto): SaleSessionResponseDto =
        saleConsolidationStarter.start(saleConsolidationSelectDto.saleIds)

    fun generate(sessionId: UUID): SaleSessionResponseDto {
        val pendingSession = saleSessionStore.load(sessionId)
        saleSessionValidator.guardAllowed(pendingSession, SaleSessionAction.GENERATE_CONSOLIDATED_SALE)
        return saleConsolidationGenerator.generate(
            ConsolidationGenerateInput(
                sourceSales = pendingSession.requiredPendingConsolidation().sourceSales,
                reviewedProductLines = ReviewedProductLines.of(pendingSession),
                pendingSessionId = sessionId,
            )
        )
    }
}
