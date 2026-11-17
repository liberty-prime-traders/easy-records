package app.myliberty.easy_records.locations.rest.endpoints

import app.myliberty.easy_records.locations.business.delivery.api.PurchaseDeliveryCreateDto
import app.myliberty.easy_records.locations.business.delivery.api.PurchaseDeliveryService
import app.myliberty.easy_records.locations.business.purchase.api.PurchaseResponseDto
import app.myliberty.easy_records.locations.business.stock_revision.api.DeliveryLineRevisionInitiator
import app.myliberty.easy_records.locations.business.stock_revision.api.DeliveryLineRevisionPreviewDto
import app.myliberty.easy_records.locations.business.stock_revision.api.DeliveryLineRevisionPreviewer
import app.myliberty.easy_records.locations.business.stock_revision.api.DeliveryLineRevisionRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/deliveries")
class PurchaseDeliveryEndpoint(
  private val purchaseDeliveryService: PurchaseDeliveryService,
  private val deliveryLineRevisionPreviewer: DeliveryLineRevisionPreviewer,
  private val deliveryLineRevisionInitiator: DeliveryLineRevisionInitiator
) {

  @PostMapping("line-revisions/preview")
  fun previewLineRevision(
    @RequestBody deliveryLineRevisionRequest: DeliveryLineRevisionRequest
  ): DeliveryLineRevisionPreviewDto = deliveryLineRevisionPreviewer.preview(deliveryLineRevisionRequest)

  @PostMapping("line-revisions")
  fun reviseLine(@RequestBody deliveryLineRevisionRequest: DeliveryLineRevisionRequest): PurchaseResponseDto =
    deliveryLineRevisionInitiator.initiateDeliveryLineRevision(deliveryLineRevisionRequest)

  @PostMapping
  fun recordDelivery(@RequestBody dto: PurchaseDeliveryCreateDto): PurchaseResponseDto =
    purchaseDeliveryService.recordDelivery(dto)
}
