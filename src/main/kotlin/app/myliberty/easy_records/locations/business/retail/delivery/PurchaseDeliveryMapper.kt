package app.myliberty.easy_records.locations.business.retail.delivery

import app.myliberty.easy_records.locations.business.retail.delivery.api.PurchaseDeliveryCreateDto
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseLineDto
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import app.myliberty.easy_records.messaging.kafka.transaction.events.PurchaseDeliveredLineDto
import app.myliberty.easy_records.util.business.Money
import java.time.Instant
import java.util.UUID

object PurchaseDeliveryMapper {

  fun toEntity(purchaseDeliveryCreateDto: PurchaseDeliveryCreateDto) = PurchaseDeliveryEntity(
    purchaseId = purchaseDeliveryCreateDto.purchaseId,
    deliveredAt = purchaseDeliveryCreateDto.deliveredAt,
    notes = purchaseDeliveryCreateDto.notes
  )

  fun toLineEntities(deliveryId: UUID, purchaseDeliveryCreateDto: PurchaseDeliveryCreateDto, storageUnitsPerUserProvidedUnitByPurchaseLineId: Map<UUID, Long>) =
    purchaseDeliveryCreateDto.lines.map { lineDto ->
      PurchaseDeliveryLineEntity(
        purchaseDeliveryId = deliveryId,
        purchaseLineId = lineDto.purchaseLineId,
        userProvidedQuantityDelivered = lineDto.userProvidedQuantityDelivered.toBigDecimal(),
        unitCost = lineDto.unitCost.toBigDecimal(),
        unitId = lineDto.unitId,
        storageUnitsPerUserProvidedUnit = storageUnitsPerUserProvidedUnitByPurchaseLineId.getValue(lineDto.purchaseLineId)
      )
    }

  fun toEvent(
    purchaseId: UUID,
    purchaseReferenceNumber: String,
    supplierId: UUID,
    deliveryRecord: DeliveryRecord,
    purchaseLineById: Map<UUID, PurchaseLineDto>,
    sourceContext: EventSourceContext.LocationLevel
  ) = PurchaseDeliveredEvent(
    eventId = UUID.randomUUID(),
    sourceContext = sourceContext,
    timestamp = Instant.now(),
    correlationId = null,
    purchaseId = purchaseId,
    deliveryId = deliveryRecord.delivery.id!!,
    deliveryReferenceNumber = deliveryRecord.delivery.requiredReference(),
    deliveredAt = deliveryRecord.delivery.deliveredAt,
    supplierId = supplierId,
    lines = deliveryRecord.lines.map { dl ->
      val pl = purchaseLineById[dl.purchaseLineId]!!
      PurchaseDeliveredLineDto(
        deliveryLineId = dl.id!!,
        lineReferenceNumber = dl.requiredReference(),
        locationProductId = pl.locationProductId,
        userProvidedQuantityDelivered = dl.userProvidedQuantityDelivered,
        unitId = dl.unitId,
        storageUnitsPerUserProvidedUnit = dl.storageUnitsPerUserProvidedUnit,
        unitCost = dl.unitCost
      )
    },
    deliveryTotal = Money.of(deliveryRecord.lines.sumOf { it.lineTotal() }),
    purchaseReferenceNumber = purchaseReferenceNumber
  )
}
