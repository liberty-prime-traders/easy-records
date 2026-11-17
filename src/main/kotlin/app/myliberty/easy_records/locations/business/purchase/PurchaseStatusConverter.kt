package app.myliberty.easy_records.locations.business.purchase

import jakarta.persistence.Converter
import app.myliberty.easy_records.locations.business.purchase.api.PurchaseStatus
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class PurchaseStatusConverter : EnumConverter<PurchaseStatus>(PurchaseStatus::class.java)
