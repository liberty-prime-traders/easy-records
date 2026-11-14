package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleHeaderDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineDto
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentDto
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionHeader
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import me.ezra_home.retail_software_solution.util.business.StringUtils
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
        return buildCopiedSession(sessionId, copiedHeader, listOf(copiedContent))
    }

    fun copyFromSalesForConsolidation(sessionId: UUID, sourceSaleHeaders: List<SaleHeaderDto>): SaleSession {
        saleSessionValidator.guardConsolidationSelectionSize(sourceSaleHeaders)
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
        return buildCopiedSession(sessionId, copiedHeader, copiedContents)
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
        copiedContents: List<CopiedSaleContent>,
    ): SaleSession {
        val saleSessionBuilder = SaleSessionBuilder(sessionId)
            .withHeader(copiedHeader)
            .withSaleLines(copiedContents.flatMap { it.saleLines })
            .withSaleAdjustments(copiedContents.flatMap { it.saleAdjustments })
            .withLinesAwaitingReservation()
        return SaleSessionTotalsCalculator.recompute(saleSessionStockOverlay.populate(saleSessionBuilder.build()))
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
