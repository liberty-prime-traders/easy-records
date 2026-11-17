package app.myliberty.easy_records.locations.business.retail.delivery.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.delivery.DeliveryHandlerForKafka
import app.myliberty.easy_records.locations.business.retail.delivery.DeliveryRecord
import app.myliberty.easy_records.locations.business.retail.delivery.PurchaseDeliveryLineRepository
import app.myliberty.easy_records.locations.business.retail.delivery.PurchaseDeliveryMapper
import app.myliberty.easy_records.locations.business.retail.delivery.PurchaseDeliveryRepository
import app.myliberty.easy_records.locations.business.retail.delivery.PurchaseDeliveryValidator
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductUnitRequestDto
import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.DeliveryHandlerForPurchase
import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.DeliveryLineQuantity
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseLineDto
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseResponseDto
import app.myliberty.easy_records.locations.business.retail.supplier_payment.api.PurchasePaymentStatusService
import app.myliberty.easy_records.organizations.business.core.fiscal_period.api.FiscalPeriodService
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.quantity.ExactQuantity
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class PurchaseDeliveryService(
  private val deliveryHandlerForPurchase: DeliveryHandlerForPurchase,
  private val deliveryRepository: PurchaseDeliveryRepository,
  private val deliveryLineRepository: PurchaseDeliveryLineRepository,
  private val deliveryHandlerForKafka: DeliveryHandlerForKafka,
  private val purchaseDeliveryValidator: PurchaseDeliveryValidator,
  private val locationProductDataFetcher: LocationProductDataFetcher,
  private val purchasePaymentStatusService: PurchasePaymentStatusService,
  private val fiscalPeriodService: FiscalPeriodService
) {

  fun recordDelivery(purchaseDeliveryCreateDto: PurchaseDeliveryCreateDto): PurchaseResponseDto {
    val deliveredDate = DateTimes.Local.atOrganizationZone(purchaseDeliveryCreateDto.deliveredAt)
    DateTimes.Local.requireNotInFuture(deliveredDate, "Delivery date")
    fiscalPeriodService.requireOpenForDate(deliveredDate)
    val context = deliveryHandlerForPurchase.prepareForDelivery(purchaseDeliveryCreateDto.purchaseId)
    purchaseDeliveryValidator.validate(purchaseDeliveryCreateDto, context.purchaseLineById)
    val storageUnitsPerUserProvidedUnitByPurchaseLineId = captureStorageUnitsPerUserProvidedUnit(
      purchaseDeliveryCreateDto, context.purchaseLineById
    )
    purchaseDeliveryValidator.ensurePendingDeliveryQuantityIsNotExceeded(
      purchaseDeliveryCreateDto,
      context.purchaseLineById,
      storageUnitsPerUserProvidedUnitByPurchaseLineId
    )

    val deliveryLines = purchaseDeliveryCreateDto.lines.map { line ->
      DeliveryLineQuantity(
        line.purchaseLineId,
        ExactQuantity.ofStorageFromUserProvided(
          line.userProvidedQuantityDelivered,
          storageUnitsPerUserProvidedUnitByPurchaseLineId.getValue(line.purchaseLineId)
        )
      )
    }

    val deliveryRecord = persistDelivery(purchaseDeliveryCreateDto, storageUnitsPerUserProvidedUnitByPurchaseLineId)
    val response = deliveryHandlerForPurchase.commitDelivery(purchaseDeliveryCreateDto.purchaseId, deliveryLines)
    deliveryHandlerForKafka.publish(context, deliveryRecord)
    return response.copy(
      paymentStatus = purchasePaymentStatusService.patchThenReturnPaymentStatus(
        purchaseDeliveryCreateDto.purchaseId,
        response.paymentCeiling
      )
    )
  }

  private fun captureStorageUnitsPerUserProvidedUnit(
    purchaseDeliveryCreateDto: PurchaseDeliveryCreateDto,
    purchaseLineById: Map<UUID, PurchaseLineDto>
  ): Map<UUID, Long> {
    val storageUnitsPerUserProvidedUnitByLocationProductId = locationProductDataFetcher.getStorageUnitsPerUserProvidedUnit(
      purchaseDeliveryCreateDto.lines.map {
        LocationProductUnitRequestDto(purchaseLineById.getValue(it.purchaseLineId).locationProductId, it.unitId)
      }
    )
    return purchaseDeliveryCreateDto.lines.associate {
      val locationProductId = purchaseLineById.getValue(it.purchaseLineId).locationProductId
      it.purchaseLineId to storageUnitsPerUserProvidedUnitByLocationProductId.getValue(locationProductId)
    }
  }

  private fun persistDelivery(
    purchaseDeliveryCreateDto: PurchaseDeliveryCreateDto,
    storageUnitsPerUserProvidedUnitByPurchaseLineId: Map<UUID, Long>
  ): DeliveryRecord {
    val delivery = deliveryRepository.save(PurchaseDeliveryMapper.toEntity(purchaseDeliveryCreateDto))
    val lines = PurchaseDeliveryMapper.toLineEntities(
      delivery.id!!,
      purchaseDeliveryCreateDto,
      storageUnitsPerUserProvidedUnitByPurchaseLineId
    )
    deliveryLineRepository.saveAll(lines)
    return DeliveryRecord(delivery, lines)
  }
}
