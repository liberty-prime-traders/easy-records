package app.myliberty.easy_records.locations.business.retail.purchase

import jakarta.persistence.Converter
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseStatus
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class PurchaseStatusConverter : EnumConverter<PurchaseStatus>(PurchaseStatus::class.java)
