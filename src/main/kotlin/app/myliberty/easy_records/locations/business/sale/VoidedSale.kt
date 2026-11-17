package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.util.business.DateTimes
import java.time.LocalDate

data class VoidedSale(
    val sale: SaleEntity,
    val voidEntity: SaleVoidEntity,
) {
    val voidDate: LocalDate = DateTimes.Local.atOrganizationZone(voidEntity.requiredCreatedOn())
}
