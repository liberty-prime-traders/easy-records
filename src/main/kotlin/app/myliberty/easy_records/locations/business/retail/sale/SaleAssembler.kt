package app.myliberty.easy_records.locations.business.retail.sale

import app.myliberty.easy_records.locations.business.retail.sale.api.SaleConsolidationEligibility
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleSummary
import app.myliberty.easy_records.locations.business.retail.sale_payment.api.SalePaymentFetcher
import app.myliberty.easy_records.util.business.mappers.NameResolution
import app.myliberty.easy_records.util.business.mappers.UserQualifier
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class SaleAssembler(
    private val nameResolution: NameResolution,
    private val userQualifier: UserQualifier,
    private val salePaymentFetcher: SalePaymentFetcher,
) {

    fun buildSummaries(saleEntities: List<SaleEntity>): List<SaleSummary> {
        val saleIds = saleEntities.map { it.id!! }
        val contactNameMap = nameResolution.organizationContacts(saleEntities.map { it.contactId })
        val paidAmountBySaleId = salePaymentFetcher.calculatePaidAmounts(saleIds)
        return saleEntities.map { saleEntity ->
            val totalPaid = paidAmountBySaleId[saleEntity.id!!] ?: BigDecimal.ZERO
            buildSummary(saleEntity, contactNameMap, totalPaid)
        }
    }

    fun buildSummary(saleEntity: SaleEntity): SaleSummary {
        val contactNameMap = nameResolution.organizationContacts(listOf(saleEntity.contactId))
        val totalPaid = salePaymentFetcher.calculatePaidAmount(saleEntity.id!!)
        return buildSummary(saleEntity, contactNameMap, totalPaid)
    }

    private fun buildSummary(
        saleEntity: SaleEntity,
        contactNameMap: Map<UUID, String>,
        totalPaid: BigDecimal
    ): SaleSummary {
        return SaleSummary(
            id = saleEntity.id!!,
            referenceNumber = saleEntity.requiredReference(),
            contactName = contactNameMap.getValue(saleEntity.contactId),
            soldBy = userQualifier.getUserFullName(saleEntity.soldById),
            dateSold = saleEntity.dateSold,
            status = saleEntity.status,
            paymentStatus = saleEntity.paymentStatus,
            arrearsTotal = SaleArrears.arrearsTotal(saleEntity.status, saleEntity.receivableTotal(), totalPaid),
            totalPaid = totalPaid,
            canConsolidate = SaleConsolidationEligibility.canConsolidate(saleEntity.status, saleEntity.contactId),
        )
    }
}
