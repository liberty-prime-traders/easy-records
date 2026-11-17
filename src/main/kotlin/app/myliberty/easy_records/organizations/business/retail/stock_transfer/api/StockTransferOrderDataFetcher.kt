package app.myliberty.easy_records.organizations.business.stock_transfer.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.organizations.business.stock_transfer.StockTransferOrderEntity
import app.myliberty.easy_records.organizations.business.stock_transfer.StockTransferOrderRepository
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnOrganizationSchema(readOnly = true)
class StockTransferOrderDataFetcher(
    private val stockTransferOrderRepository: StockTransferOrderRepository
) {

    fun getById(id: UUID): StockTransferOrderDomainDto =
        stockTransferOrderRepository.findById(id)
            .orElseThrow { RtsGenericException("Stock transfer order $id not found") }
            .toDomainDto()

    fun getByReferenceNumber(referenceNumber: String): StockTransferOrderDomainDto =
        stockTransferOrderRepository.findByReferenceNumber(referenceNumber)?.toDomainDto()
            ?: throw RtsGenericException("Stock transfer order $referenceNumber not found")

    fun getTopByLocationId(limit: Int): List<StockTransferOrderDomainDto> {
        val locationId = SessionContextProvider.getLocationId()
        return stockTransferOrderRepository
            .findTopNByLocation(locationId, PageRequest.of(0, limit))
            .map { it.toDomainDto() }
    }

    fun getTopForOrganization(limit: Int): List<StockTransferOrderDomainDto> =
        stockTransferOrderRepository
            .findAllByOrderByCreatedOnDesc(PageRequest.of(0, limit))
            .map { it.toDomainDto() }

    private fun StockTransferOrderEntity.toDomainDto() = StockTransferOrderDomainDto(
        id = id!!,
        referenceNumber = requiredReference(),
        sourceLocationId = sourceLocationId,
        destinationLocationId = destinationLocationId,
        status = status,
        notes = notes,
        lineCount = lineCount,
        totalDispatchedCost = totalDispatchedCost,
        dispatchedAt = dispatchedAt,
        dispatchedByName = dispatchedByName,
        createdById = requiredCreatedById(),
        createdOn = requiredCreatedOn()
    )
}
