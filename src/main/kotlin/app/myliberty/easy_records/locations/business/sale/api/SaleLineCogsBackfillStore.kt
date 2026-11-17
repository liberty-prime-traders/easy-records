package app.myliberty.easy_records.locations.business.sale.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.sale.SaleLineCogsEntity
import app.myliberty.easy_records.locations.business.sale.SaleLineCogsRepository
import app.myliberty.easy_records.locations.business.sale.SaleLineRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class SaleLineCogsBackfillStore(
    private val saleLineRepository: SaleLineRepository,
    private val saleLineCogsRepository: SaleLineCogsRepository,
) {

    fun findLinesMissingCogsRecord(batchSize: Int): List<SaleLineCogsBackfillCandidate> =
        saleLineRepository.findLinesMissingCogsRecord(PageRequest.of(0, batchSize))

    fun insertCogsRecords(candidates: List<SaleLineCogsBackfillCandidate>, referenceNumbers: List<String>) {
        saleLineCogsRepository.saveAllAndFlush(candidates.mapIndexed { index, candidate ->
            SaleLineCogsEntity(saleLineId = candidate.saleLineId, cogsTotal = candidate.cogsTotal).also {
                it.referenceNumber = referenceNumbers[index]
                it.createdOn = candidate.dateSold
            }
        })
    }

    fun countSaleLinesWithCogsTotal(): Long = saleLineRepository.countByCogsTotalIsNotNull()

    fun countCogsRecords(): Long = saleLineCogsRepository.count()
}
