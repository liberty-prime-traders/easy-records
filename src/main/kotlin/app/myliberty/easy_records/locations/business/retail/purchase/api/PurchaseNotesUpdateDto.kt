package app.myliberty.easy_records.locations.business.retail.purchase.api

import java.io.Serializable
import java.util.Optional

data class PurchaseNotesUpdateDto(val notes: Optional<String>?) : Serializable
