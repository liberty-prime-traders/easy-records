package app.myliberty.easy_records.locations.business.retail.purchase

import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseCreateDto
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseLineCreateDto
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseLineDto
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseStatus
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseUpdateDto
import app.myliberty.easy_records.util.business.DateTimes
import java.util.UUID

object PurchaseMapper {

  fun toDraftEntity(purchaseCreateDto: PurchaseCreateDto) = PurchaseEntity(
    supplierId = purchaseCreateDto.supplierId,
    notes = purchaseCreateDto.notes,
    dateOrdered = purchaseCreateDto.dateOrdered,
    orderedById = purchaseCreateDto.orderedById
  )

  fun toOrderEntity(purchaseCreateDto: PurchaseCreateDto) = PurchaseEntity(
    supplierId = purchaseCreateDto.supplierId,
    notes = purchaseCreateDto.notes,
    purchaseStatus = PurchaseStatus.ORDERED,
    dateOrdered = purchaseCreateDto.dateOrdered ?: DateTimes.Offset.Now.organization(),
    orderedById = purchaseCreateDto.orderedById ?: SessionContextProvider.getUserId()
  )

  fun applyDraftUpdate(purchase: PurchaseEntity, purchaseUpdateDto: PurchaseUpdateDto) {
    purchaseUpdateDto.supplierId?.let { purchase.supplierId = it }
    purchaseUpdateDto.notes?.let { purchase.notes = it.orElse(null) }
    purchaseUpdateDto.dateOrdered?.let { purchase.dateOrdered = it.orElse(null) }
    purchaseUpdateDto.orderedById?.let { purchase.orderedById = it.orElse(null) }
  }

  fun convertDraftToOrder(purchase: PurchaseEntity, purchaseUpdateDto: PurchaseUpdateDto) {
    purchaseUpdateDto.supplierId?.let { purchase.supplierId = it }
    purchaseUpdateDto.notes?.let { purchase.notes = it.orElse(null) }
    purchase.dateOrdered = purchaseUpdateDto.dateOrdered?.orElse(null) ?: DateTimes.Offset.Now.organization()
    purchase.orderedById = purchaseUpdateDto.orderedById?.orElse(null) ?: SessionContextProvider.getUserId()
    purchase.purchaseStatus = PurchaseStatus.ORDERED
  }

  fun toNewLineEntity(purchaseId: UUID, purchaseLineCreateDto: PurchaseLineCreateDto, storageUnitsPerUserProvidedUnit: Long) = PurchaseLineEntity(
    purchaseId = purchaseId,
    locationProductId = purchaseLineCreateDto.locationProductId,
    userProvidedQuantityOrdered = purchaseLineCreateDto.userProvidedQuantityOrdered.toBigDecimal(),
    unitCost = purchaseLineCreateDto.unitCost.toBigDecimal(),
    unitId = purchaseLineCreateDto.unitId,
    storageUnitsPerUserProvidedUnit = storageUnitsPerUserProvidedUnit
  )

  fun toLineEntities(purchaseId: UUID, lines: List<PurchaseLineCreateDto>, storageUnitsPerUserProvidedUnitByProductId: Map<UUID, Long>) =
    lines.map { toNewLineEntity(purchaseId, it, storageUnitsPerUserProvidedUnitByProductId.getValue(it.locationProductId)) }

  fun purchaseLineEntityToDto(purchaseLineEntity: PurchaseLineEntity) = PurchaseLineDto(
    id = purchaseLineEntity.id!!,
    purchaseId = purchaseLineEntity.purchaseId,
    locationProductId = purchaseLineEntity.locationProductId,
    unitCost = purchaseLineEntity.unitCost,
    unitId = purchaseLineEntity.unitId,
    storageUnitsPerUserProvidedUnit = purchaseLineEntity.storageUnitsPerUserProvidedUnit,
    storageNetOrderedQty = purchaseLineEntity.storageNetOrderedQty(),
    storagePendingDeliveryQty = purchaseLineEntity.storagePendingDeliveryQty()
  )
}
