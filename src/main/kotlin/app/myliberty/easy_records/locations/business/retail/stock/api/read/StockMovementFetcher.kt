package app.myliberty.easy_records.locations.business.retail.stock.api.read

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.stock.StockMovementRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class StockMovementFetcher(
    private val stockMovementRepository: StockMovementRepository,
) {

    fun findOnEntry(stockEntryId: UUID): List<StockMovementDto> =
        stockMovementRepository.findByStockEntryId(stockEntryId).map { StockMovementDto.of(it) }

    fun findOfRevision(stockRevisionReference: String): List<StockMovementDto> =
        stockMovementRepository.findByStockRevisionReference(stockRevisionReference).map { StockMovementDto.of(it) }

    fun findOfDocuments(locationProductId: UUID, documentReferences: Set<String>): List<StockMovementDto> =
        stockMovementRepository.findByLocationProductIdAndDocumentReferenceIn(locationProductId, documentReferences)
            .map { StockMovementDto.of(it) }
}
