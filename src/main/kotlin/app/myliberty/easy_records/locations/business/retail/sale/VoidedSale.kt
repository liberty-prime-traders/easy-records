package app.myliberty.easy_records.locations.business.sale

import app.myliberty.easy_records.util.business.DateTimes
import java.time.LocalDate

data class VoidedSale(
    val sale: SaleEntity,
    val voidEntity: SaleVoidEntity,
) {
    val voidDate: LocalDate = DateTimes.Local.atOrganizationZone(voidEntity.requiredCreatedOn())
}
