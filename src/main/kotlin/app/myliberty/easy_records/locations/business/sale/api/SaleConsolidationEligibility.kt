package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.util.enums.SystemContact
import java.util.UUID

object SaleConsolidationEligibility {

    fun statusAllows(saleStatus: SaleStatus): Boolean =
        saleStatus == SaleStatus.DRAFT || saleStatus == SaleStatus.CONFIRMED

    fun isWalkInSale(contactId: UUID): Boolean = contactId == SystemContact.WALK_IN.id

    fun canConsolidate(saleStatus: SaleStatus, contactId: UUID): Boolean =
        statusAllows(saleStatus) && !isWalkInSale(contactId)
}
