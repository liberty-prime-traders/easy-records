package app.myliberty.easy_records.locations.business.delivery.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.delivery.PurchaseDeliveryAssembler
import app.myliberty.easy_records.locations.business.delivery.PurchaseDeliveryLineRepository
import app.myliberty.easy_records.locations.business.delivery.PurchaseDeliveryRepository
import app.myliberty.easy_records.locations.business.location_product.api.LocationProductSummaryDto
import app.myliberty.easy_records.locations.business.purchase.api.delivery.PurchaseDeliveryLineStatus
import app.myliberty.easy_records.locations.business.purchase.api.PurchaseLineDto
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import app.myliberty.easy_records.util.quantity.ExactQuantity
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class PurchaseDeliveryDataFetcher(
    private val purchaseDeliveryRepository: PurchaseDeliveryRepository,
    private val purchaseDeliveryLineRepository: PurchaseDeliveryLineRepository,
    private val purchaseDeliveryAssembler: PurchaseDeliveryAssembler
) {

    fun getDeliveryReferenceNumbersById(deliveryIds: List<UUID>): Map<UUID, String> {
        return purchaseDeliveryRepository.findAllById(deliveryIds)
            .mapNotNull { entity -> entity.referenceNumber?.let { entity.id!! to it } }
            .toMap()
    }

    fun getRevisionTotals(
        revisedLineReferenceNumber: String,
        replacementLineReferenceNumber: String
    ): DeliveryLineRevisionTotalsDto {
        val lineByReferenceNumber = purchaseDeliveryLineRepository
            .findByReferenceNumberIn(listOf(revisedLineReferenceNumber, replacementLineReferenceNumber))
            .associateBy { it.requiredReference() }
        val revisedLine = lineByReferenceNumber[revisedLineReferenceNumber]
            ?: throw RtsGenericException("Delivery line $revisedLineReferenceNumber not found")
        val replacementLine = lineByReferenceNumber[replacementLineReferenceNumber]
            ?: throw RtsGenericException("Delivery line $replacementLineReferenceNumber not found")
        return DeliveryLineRevisionTotalsDto(
            purchaseId = purchaseDeliveryRepository.getReferenceById(replacementLine.purchaseDeliveryId).purchaseId,
            revisedLineTotal = revisedLine.lineTotal(),
            replacementLineTotal = replacementLine.lineTotal(),
        )
    }

    fun getOwners(deliveryLineId: UUID): DeliveryLineOwnersDto =
        purchaseDeliveryLineRepository.findOwnersById(deliveryLineId)
            ?.let { DeliveryLineOwnersDto(it.purchaseId, it.purchaseLineId) }
            ?: throw RtsGenericException("That delivery line could not be found. Refresh the page and try again.")

    fun calculateDeliveredTotals(purchaseIds: Set<UUID>): Map<UUID, BigDecimal> {
        val purchaseIdByDeliveryId = purchaseDeliveryRepository.findByPurchaseIdIn(purchaseIds)
            .associate { it.id!! to it.purchaseId }
        if (purchaseIdByDeliveryId.isEmpty()) return purchaseIds.associateWith { BigDecimal.ZERO }
        val deliveredTotalsByPurchaseId = purchaseDeliveryLineRepository
            .findByPurchaseDeliveryIdInAndStatus(
                purchaseIdByDeliveryId.keys.toList(), PurchaseDeliveryLineStatus.ACTIVE
            )
            .groupBy { purchaseIdByDeliveryId.getValue(it.purchaseDeliveryId) }
            .mapValues { (_, lines) -> lines.sumOf { it.lineTotal() } }
        return purchaseIds.associateWith { deliveredTotalsByPurchaseId[it] ?: BigDecimal.ZERO }
    }

    fun calculateSingleDeliveryTotal(deliveryId: UUID): BigDecimal {
        val lines = purchaseDeliveryLineRepository.findByPurchaseDeliveryIdInAndStatus(
            listOf(deliveryId),
            PurchaseDeliveryLineStatus.ACTIVE
        )
        return lines.sumOf { it.lineTotal() }
    }

    fun calculateStorageQuantityDelivered(purchaseLineId: UUID): ExactQuantity {
        val activeLines = purchaseDeliveryLineRepository.findByPurchaseLineIdAndStatus(
            purchaseLineId,
            PurchaseDeliveryLineStatus.ACTIVE
        )
        return activeLines.fold(ExactQuantity.ZERO) { total, line ->
            total + ExactQuantity.ofStorageFromUserProvided(
                line.userProvidedQuantityDelivered,
                line.storageUnitsPerUserProvidedUnit
            )
        }
    }

    fun getDeliveryResponses(
        purchaseIds: List<UUID>,
        purchaseLineById: Map<UUID, PurchaseLineDto>,
        productSummaries: Map<UUID, LocationProductSummaryDto>
    ): Map<UUID, List<PurchaseDeliveryResponseDto>> {
        val allDeliveries = purchaseDeliveryRepository.findByPurchaseIdIn(purchaseIds)
        val deliveriesByPurchaseId = allDeliveries.groupBy { it.purchaseId }
        return deliveriesByPurchaseId.mapValues { (_, deliveries) ->
            purchaseDeliveryAssembler.buildResponses(deliveries, purchaseLineById, productSummaries)
        }
    }
}
