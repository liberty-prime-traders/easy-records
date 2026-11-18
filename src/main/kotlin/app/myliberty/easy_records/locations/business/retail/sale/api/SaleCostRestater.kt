package app.myliberty.easy_records.locations.business.retail.sale.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.sale.SaleLineCogsEntity
import app.myliberty.easy_records.locations.business.retail.sale.SaleLineCogsRepository
import app.myliberty.easy_records.locations.business.retail.sale.SaleLineRepository
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation

// Appends cost rows; a sale's revenue, lines and status are never edited.
@Service
@TransactionalOnLocationSchema
class SaleCostRestater(
    private val saleLineRepository: SaleLineRepository,
    private val saleLineCogsRepository: SaleLineCogsRepository,
) {

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun restate(saleLineCostChanges: List<SaleLineCostChange>) {
        if (saleLineCostChanges.isEmpty()) return
        val saleLineByReferenceNumber = saleLineRepository
            .findByReferenceNumberIn(saleLineCostChanges.map { it.saleLineReferenceNumber })
            .associateBy { it.requiredReference() }
        val activeCogsBySaleLineId = saleLineCogsRepository
            .findBySaleLineIdInAndStatus(saleLineByReferenceNumber.values.map { it.id!! }, SaleLineCogsStatus.ACTIVE)
            .associateBy { it.saleLineId }

        saleLineCostChanges.forEach { saleLineCostChange ->
            val saleLine = saleLineByReferenceNumber[saleLineCostChange.saleLineReferenceNumber]
                ?: throw LibertyGenericException("Sale line ${saleLineCostChange.saleLineReferenceNumber} not found")
            val activeCogs = activeCogsBySaleLineId[saleLine.id!!]
                ?: throw LibertyGenericException("Sale line ${saleLineCostChange.saleLineReferenceNumber} has no cost record")
            val restatedCogsTotal = activeCogs.cogsTotal - saleLineCostChange.oldCost + saleLineCostChange.newCost
            if (restatedCogsTotal.signum() < 0) {
                throw LibertyGenericException(
                    "Sale line ${saleLineCostChange.saleLineReferenceNumber} would have a negative cost after the revision"
                )
            }
            // Flushed first: the unique index on ACTIVE rows is not deferrable and inserts flush before updates.
            activeCogs.status = SaleLineCogsStatus.REPLACED
            saleLineCogsRepository.saveAndFlush(activeCogs)
            saleLineCogsRepository.save(SaleLineCogsEntity(saleLineId = saleLine.id!!, cogsTotal = restatedCogsTotal))
        }
    }
}
