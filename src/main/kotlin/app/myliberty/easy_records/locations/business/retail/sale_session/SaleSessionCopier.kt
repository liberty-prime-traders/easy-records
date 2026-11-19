package app.myliberty.easy_records.locations.business.retail.sale_session

import app.myliberty.easy_records.locations.business.retail.sale.api.SaleDataFetcher
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleHeaderDto
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleLineDto
import app.myliberty.easy_records.locations.business.retail.sale_adjustment.api.SaleAdjustmentDto
import app.myliberty.easy_records.locations.business.retail.sale_adjustment.api.SaleAdjustmentFetcher
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSession
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionAdjustment
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionHeader
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SessionIdentity
import app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.ConsolidationCopy
import app.myliberty.easy_records.util.business.StringUtils
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SaleSessionCopier(
    private val saleDataFetcher: SaleDataFetcher,
    private val saleAdjustmentFetcher: SaleAdjustmentFetcher,
    private val domainToSessionMapper: DomainToSessionMapper,
    private val saleSessionStockOverlay: SaleSessionStockOverlay,
    private val saleSessionValidator: SaleSessionValidator,
) {

    fun copyFromSale(sessionId: UUID, sourceSaleId: UUID): SaleSession {
        val sourceSaleHeader = saleDataFetcher.getSaleHeader(sourceSaleId)
        saleSessionValidator.guardSourceSaleCopyable(sourceSaleHeader.status)
        val copiedContent = copySaleContent(
            sourceSaleLines = saleDataFetcher.getSaleLines(sourceSaleId),
            sourceSaleAdjustments = saleAdjustmentFetcher.getAdjustments(sourceSaleId),
        )
        val copiedHeader = SaleSessionHeader(
            contactId = sourceSaleHeader.contactId,
            soldById = null,
            dateSold = null,
            notes = sourceSaleHeader.notes,
            referenceNumber = null,
        )
        return buildCopiedSession(
            sessionId, copiedHeader, SaleLineRanker.alphabetical(copiedContent.saleLines), copiedContent.saleAdjustments
        )
    }

    fun copyFromSalesWithSourceReferences(
        sessionId: UUID,
        sourceSaleHeaders: List<SaleHeaderDto>,
    ): ConsolidationCopy {
        saleSessionValidator.guardConsolidationSelectionSize(sourceSaleHeaders.size)
        saleSessionValidator.guardSingleContactAcrossSales(sourceSaleHeaders)
        val sourceSaleIds = sourceSaleHeaders.map { it.id }
        val sourceSaleLinesBySaleId = saleDataFetcher.getSaleLinesBySaleId(sourceSaleIds)
        val sourceSaleAdjustmentsBySaleId = saleAdjustmentFetcher.getAdjustmentsBySaleId(sourceSaleIds)
        val copiedContents = sourceSaleHeaders.map { sourceSaleHeader ->
            copySaleContent(
                sourceSaleLines = sourceSaleLinesBySaleId.getValue(sourceSaleHeader.id),
                sourceSaleAdjustments = sourceSaleAdjustmentsBySaleId.getValue(sourceSaleHeader.id),
            )
        }
        val consolidatedNotes = sourceSaleHeaders
            .mapNotNull { sourceSaleHeader ->
                StringUtils.getValueOrNull(sourceSaleHeader.notes)?.let {
                    notes -> "${sourceSaleHeader.referenceNumber} - $notes"
                }
            }
            .joinToString(separator = "\n")
            .ifEmpty { null }
        val copiedHeader = SaleSessionHeader(
            contactId = sourceSaleHeaders.first().contactId,
            soldById = null,
            dateSold = null,
            notes = consolidatedNotes,
            referenceNumber = null,
        )
        val consolidationSession = buildCopiedSession(
            sessionId,
            copiedHeader,
            SaleLineRanker.alphabeticalWithLikeProductsTogether(copiedContents.flatMap { it.saleLines }),
            copiedContents.flatMap { it.saleAdjustments },
        )
        val sourceSaleReferenceByLineKey = sourceSaleHeaders.indices.flatMap { sourceIndex ->
            val sourceSaleReference = sourceSaleHeaders[sourceIndex].referenceNumber
            copiedContents[sourceIndex].saleLines.map { it.lineKey to sourceSaleReference }
        }.toMap()
        val orderLevelAmountByAdjustmentKey = copiedContents.flatMap { copiedContent ->
            copiedContent.saleAdjustments
                .filter { it.relatedSaleLineIdentity == null }
                .map { orderLevelAdjustment ->
                    orderLevelAdjustment.identity.key() to SaleSessionTotalsCalculator
                        .calculateAdjustmentAmount(orderLevelAdjustment, copiedContent.saleLines)
                }
        }.toMap()
        val saleReferencesWithOrderLevelAdjustments = sourceSaleHeaders.indices
            .filter { sourceIndex ->
                copiedContents[sourceIndex].saleAdjustments.any { it.relatedSaleLineIdentity == null }
            }
            .map { sourceSaleHeaders[it].referenceNumber }
            .toSet()
        return ConsolidationCopy(
            consolidationSession,
            sourceSaleReferenceByLineKey,
            sourceSaleLinesBySaleId,
            orderLevelAmountByAdjustmentKey,
            saleReferencesWithOrderLevelAdjustments,
        )
    }

    private fun copySaleContent(
        sourceSaleLines: List<SaleLineDto>,
        sourceSaleAdjustments: List<SaleAdjustmentDto>,
    ): CopiedSaleContent {
        val sourcedSaleLines = sourceSaleLines.map { sourceSaleLine ->
            val copiedSaleLine = domainToSessionMapper.toSaleSessionLine(sourceSaleLine)
                .copy(identity = SessionIdentity.mintFreshIdentity())
            SourcedSaleLine(sourceSaleLineId = sourceSaleLine.id, copiedSaleLine = copiedSaleLine)
        }
        val copiedSaleLines = sourcedSaleLines.map { it.copiedSaleLine }
        val copiedLineIdentityBySourceLineId = sourcedSaleLines
            .associate { it.sourceSaleLineId to it.copiedSaleLine.identity }
        val copiedSaleAdjustments = sourceSaleAdjustments.map { sourceSaleAdjustment ->
            domainToSessionMapper.toSaleSessionAdjustment(sourceSaleAdjustment, copiedLineIdentityBySourceLineId)
                .copy(identity = SessionIdentity.mintFreshIdentity())
        }
        return CopiedSaleContent(saleLines = copiedSaleLines, saleAdjustments = copiedSaleAdjustments)
    }

    private fun buildCopiedSession(
        sessionId: UUID,
        copiedHeader: SaleSessionHeader,
        rankedSaleLines: List<SaleSessionLine>,
        copiedSaleAdjustments: List<SaleSessionAdjustment>,
    ): SaleSession {
        val saleSessionBuilder = SaleSessionBuilder(sessionId)
            .withHeader(copiedHeader)
            .withSaleLines(rankedSaleLines)
            .withSaleAdjustments(copiedSaleAdjustments)
            .withLinesAwaitingReservation()
        return saleSessionStockOverlay.populate(saleSessionBuilder.build())
    }
}

private data class SourcedSaleLine(
    val sourceSaleLineId: UUID,
    val copiedSaleLine: SaleSessionLine,
)

private data class CopiedSaleContent(
    val saleLines: List<SaleSessionLine>,
    val saleAdjustments: List<SaleSessionAdjustment>,
)
