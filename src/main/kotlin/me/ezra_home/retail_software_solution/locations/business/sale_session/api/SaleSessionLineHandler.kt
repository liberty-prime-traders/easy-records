package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleLineRanker
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionLineAdjustmentReconciler
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionLineChangeContext
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionLineChangeContextBuilder
import me.ezra_home.retail_software_solution.locations.business.sale_session.consolidation.PendingConsolidationRemovedLines
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStockOverlay
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStore
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionUpdateFinalizer
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionValidator
import me.ezra_home.retail_software_solution.locations.business.stock_reservation.api.StockReserver
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class SaleSessionLineHandler(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionValidator: SaleSessionValidator,
    private val saleSessionUpdateFinalizer: SaleSessionUpdateFinalizer,
    private val saleSessionLineChangeContextBuilder: SaleSessionLineChangeContextBuilder,
    private val saleSessionStockOverlay: SaleSessionStockOverlay,
    private val stockReserver: StockReserver,
    private val saleSessionLineAdjustmentReconciler: SaleSessionLineAdjustmentReconciler,
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun applyLineChanges(sessionId: UUID, lineRequestDto: SaleSessionLineRequestDto): SaleSessionResponseDto {
        val saleSession = saleSessionStore.load(sessionId)
        saleSessionValidator.guardAllowed(saleSession, SaleSessionAction.EDIT_LINES)
        if (lineRequestDto.additions.isNotEmpty()) {
            saleSessionValidator.guardAllowed(saleSession, SaleSessionAction.ADD_LINE)
        }

        val saleSessionLinesByKey = saleSession.saleLines.associateBy { it.identity.key() }
        guardUpdatesTargetExistingLines(lineRequestDto.updates, saleSessionLinesByKey)

        val saleSessionLineChangeContext = saleSessionLineChangeContextBuilder.buildContext(
            additions = lineRequestDto.additions,
            updates = lineRequestDto.updates,
            saleSessionLinesByKey = saleSessionLinesByKey,
        )

        val newSaleSessionLines = buildNewSaleSessionLines(
            lineRequestDto.additions,
            saleSessionLineChangeContext,
            SaleLineRanker.ranksAboveAll(saleSession.saleLines, lineRequestDto.additions.size),
        )
        val mutatedExistingLines = applyUpdatesToExistingLines(
            existingSaleLines = saleSession.saleLines,
            updates = lineRequestDto.updates,
            saleSessionLineChangeContext = saleSessionLineChangeContext,
        )
        val effectiveSaleLines = buildList {
            addAll(newSaleSessionLines)
            addAll(mutatedExistingLines)
        }
        val effectiveSaleLinesByKey = effectiveSaleLines.associateBy { it.identity.key() }
        val updatedSaleAdjustments = saleSessionLineAdjustmentReconciler.reconcile(
            preUpdateSaleAdjustments = saleSession.saleAdjustments,
            preUpdateSaleSessionLinesByKey = saleSessionLinesByKey,
            postUpdateSaleSessionLinesByKey = effectiveSaleLinesByKey,
            updates = lineRequestDto.updates,
        )
        val updatedSaleSession = saleSession.copy(
            saleLines = effectiveSaleLines,
            saleAdjustments = updatedSaleAdjustments,
            productsVersion = saleSession.productsVersion + 1,
        )
        return saleSessionUpdateFinalizer.finalize(saleSessionStockOverlay.populate(updatedSaleSession))
    }

    private fun guardUpdatesTargetExistingLines(
        updates: List<SaleSessionLineUpdateDto>,
        saleSessionLinesByKey: Map<UUID, SaleSessionLine>,
    ) {
        updates.map { it.identity.key() }
            .filterNot { saleSessionLinesByKey.containsKey(it) }
            .let {
                if (it.isNotEmpty()) {
                    throw RtsGenericException("Attempted to update a line that is not in the session")
                }
            }
    }

    private fun buildNewSaleSessionLines(
        additions: List<SaleSessionLineAddDto>,
        saleSessionLineChangeContext: SaleSessionLineChangeContext,
        ranks: List<BigDecimal>,
    ): List<SaleSessionLine> {

        return additions.mapIndexed { additionIndex, lineAddDto ->
            val productSummary = saleSessionLineChangeContext.locationProductSummariesById.getValue(lineAddDto.locationProductId)
            val defaultSalePrice = saleSessionLineChangeContext.defaultSalePricesByLocationProductId[lineAddDto.locationProductId]
                ?: throw RtsGenericException("Product ${productSummary.label} has no default sale price")
            val storageUnitsPerBaseUnit = saleSessionLineChangeContext.storageUnitsPerBaseUnitByBaseUnitId.getValue(productSummary.baseUnitId)
            SaleSessionLine(
                identity = SessionIdentity.mintFreshIdentity(),
                locationProductId = lineAddDto.locationProductId,
                productLabel = productSummary.label,
                productPrintLabel = productSummary.printLabel,
                userProvidedQuantity = lineAddDto.userProvidedQuantity.toBigDecimal(),
                unitId = productSummary.baseUnitId,
                baseUnitId = productSummary.baseUnitId,
                storageUnitsPerUserProvidedUnit = storageUnitsPerBaseUnit,
                lineTotal = SaleSessionLine.lineTotal(
                    defaultSalePrice,
                    LineQuantities(
                        userProvidedQuantity = lineAddDto.userProvidedQuantity.toBigDecimal(),
                        storageUnitsPerUserProvidedUnit = storageUnitsPerBaseUnit,
                        storageUnitsPerBaseUnit = storageUnitsPerBaseUnit,
                    ),
                ),
                defaultSalePrice = defaultSalePrice,
                rank = ranks[additionIndex],
            )
        }
    }

    private fun applyUpdatesToExistingLines(
        existingSaleLines: List<SaleSessionLine>,
        updates: List<SaleSessionLineUpdateDto>,
        saleSessionLineChangeContext: SaleSessionLineChangeContext,
    ): List<SaleSessionLine> {
        val updatesByLineKey = updates.associateBy { it.identity.key() }
        return existingSaleLines.map { saleSessionLine ->
            val lineUpdateDto = updatesByLineKey[saleSessionLine.identity.key()] ?: return@map saleSessionLine
            val storageUnitsPerUserProvidedUnit = saleSessionLineChangeContext.newStorageUnitsPerUserProvidedUnitByLineKey[saleSessionLine.identity.key()]
                ?: saleSessionLine.storageUnitsPerUserProvidedUnit
            saleSessionLine.copy(
                userProvidedQuantity = lineUpdateDto.userProvidedQuantity.toBigDecimal(),
                unitId = lineUpdateDto.unitId,
                storageUnitsPerUserProvidedUnit = storageUnitsPerUserProvidedUnit,
                lineTotal = SaleSessionLine.lineTotal(
                    saleSessionLine.defaultSalePrice,
                    LineQuantities(
                        userProvidedQuantity = lineUpdateDto.userProvidedQuantity.toBigDecimal(),
                        storageUnitsPerUserProvidedUnit = storageUnitsPerUserProvidedUnit,
                        storageUnitsPerBaseUnit = saleSessionLineChangeContext.storageUnitsPerBaseUnitByBaseUnitId.getValue(saleSessionLine.baseUnitId),
                    ),
                ),
            )
        }
    }

    @TransactionalOnLocationSchema
    fun removeLine(sessionId: UUID, rowIdentityDto: SaleSessionRowIdentityDto): SaleSessionResponseDto {
        val saleSession = saleSessionStore.load(sessionId)
        saleSessionValidator.guardAllowed(saleSession, SaleSessionAction.EDIT_LINES)
        val targetLineKey = rowIdentityDto.identity.key()
        val persistedSaleLineId = saleSession.saleLines.firstOrNull { it.identity.key() == targetLineKey }
            ?.identity?.id
        if (persistedSaleLineId != null) {
            stockReserver.clearBySaleLineIds(listOf(persistedSaleLineId))
        }
        val sessionWithoutLine = if (SaleSessionRules.keepsRemovedLines(saleSession.kind)) {
            PendingConsolidationRemovedLines.remove(saleSession, targetLineKey)
        } else {
            saleSession.withoutLine(targetLineKey)
        }
        val updatedSaleSession = sessionWithoutLine.copy(productsVersion = sessionWithoutLine.productsVersion + 1)
        return saleSessionUpdateFinalizer.finalize(saleSessionStockOverlay.populate(updatedSaleSession))
    }

    fun restoreLine(sessionId: UUID, rowIdentityDto: SaleSessionRowIdentityDto): SaleSessionResponseDto {
        val saleSession = saleSessionStore.load(sessionId)
        saleSessionValidator.guardAllowed(saleSession, SaleSessionAction.RESTORE_REMOVED_LINE)
        val updatedSaleSession = PendingConsolidationRemovedLines.restore(saleSession, rowIdentityDto.identity.key())
        return saleSessionUpdateFinalizer.finalize(saleSessionStockOverlay.populate(updatedSaleSession))
    }
}
