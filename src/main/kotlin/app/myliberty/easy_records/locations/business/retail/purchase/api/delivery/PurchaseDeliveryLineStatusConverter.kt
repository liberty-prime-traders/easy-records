package app.myliberty.easy_records.locations.business.retail.purchase.api.delivery

import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class PurchaseDeliveryLineStatusConverter : EnumConverter<PurchaseDeliveryLineStatus>(PurchaseDeliveryLineStatus::class.java)
