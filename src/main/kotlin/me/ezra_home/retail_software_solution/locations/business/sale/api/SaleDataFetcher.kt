package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.SaleAssembler
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineCogsRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLocker
import me.ezra_home.retail_software_solution.locations.business.sale.SaleRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleVoidRepository
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import java.math.BigDecimal
import java.util.UUID

data class SaleContext(val contactId: UUID, val receivableTotal: BigDecimal, val status: SaleStatus)

@Service
@TransactionalOnLocationSchema(readOnly = true)
class SaleDataFetcher(
    private val saleRepository: SaleRepository,
    private val saleLineRepository: SaleLineRepository,
    private val saleLineCogsRepository: SaleLineCogsRepository,
    private val saleAssembler: SaleAssembler,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val saleLocker: SaleLocker,
    private val saleVoidRepository: SaleVoidRepository,
    private val userQualifier: UserQualifier
) {

    fun fetchRecent(n: Int?): List<SaleSummary> {
        val recordCount = n ?: 30
        if (recordCount <= 0) throw RtsGenericException("Limit must be positive")
        if (recordCount > 1000) throw RtsGenericException("Limit exceeds maximum of 1000")
        val sort = Sort.by(Sort.Order.desc("createdOn"))
        return saleAssembler.buildSummaries(
            saleRepository.findAll(PageRequest.of(0, recordCount, sort)).content
        )
    }

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun lockAndGetSaleContext(saleId: UUID): SaleContext = lockAndGetSaleContexts(listOf(saleId)).getValue(saleId)

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun lockAndGetSaleContexts(saleIds: List<UUID>): Map<UUID, SaleContext> =
        saleLocker.lockAndGetSales(saleIds).associate { sale ->
            sale.id!! to SaleContext(sale.contactId, sale.receivableTotal(), sale.status)
        }

    fun getSaleContactId(saleId: UUID): UUID {
        return saleRepository.getReferenceById(saleId).contactId
    }

    fun getSaleCogsTotals(saleIds: Collection<UUID>): Map<UUID, BigDecimal> {
        val cogsTotalBySaleId = saleLineCogsRepository
            .sumCogsTotalsBySaleIdsAndStatus(saleIds, SaleLineCogsStatus.ACTIVE)
            .associate { it.saleId to it.cogsTotal }
        return saleIds.associateWith { cogsTotalBySaleId[it] ?: BigDecimal.ZERO }
    }

    fun getReferenceNumbersById(saleIds: Collection<UUID>): Map<UUID, String> =
        saleRepository.findAllById(saleIds).associate { it.id!! to it.requiredReference() }

    fun getSaleHeader(saleId: UUID): SaleHeaderDto = getSaleHeaders(listOf(saleId)).single()

    fun getSaleHeaders(saleIds: List<UUID>): List<SaleHeaderDto> = loadHeaders(saleIds)

    // Every lock is taken, in id order, before any sale is read, so what is read cannot change underneath the caller.
    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun lockAndGetSaleHeaders(saleIds: List<UUID>): List<SaleHeaderDto> =
        saleLocker.lockAndGetSales(saleIds).map(::extractHeader)

    fun getSaleHeaderByReferenceNumber(referenceNumber: String): SaleHeaderDto {
        val saleEntity = saleRepository.findByReferenceNumber(referenceNumber)
            ?: throw RtsGenericException("Sale $referenceNumber not found")
        return extractHeader(saleEntity)
    }

    fun getSaleVoidInfo(saleId: UUID): SaleVoidInfoDto? {
        val saleVoid = saleVoidRepository.findBySaleId(saleId) ?: return null
        return SaleVoidInfoDto(
            voidedBy = userQualifier.getCreatorFullName(saleVoid.requiredCreatedById()),
            voidedOn = saleVoid.requiredCreatedOn(),
            voidedReason = saleVoid.reason
        )
    }

    fun getSaleLines(saleId: UUID): List<SaleLineDto> =
        getSaleLinesBySaleId(listOf(saleId)).getValue(saleId)

    fun getSaleLinesBySaleId(saleIds: List<UUID>): Map<UUID, List<SaleLineDto>> {
        val lines = saleLineRepository.findBySaleIdInOrderByCreatedOnAscIdAsc(saleIds)
        val productSummaries = locationProductDataFetcher.findSummaryByIds(lines.map { it.locationProductId })
        val saleLineDtosBySaleId = lines.groupBy({ it.saleId }, { line ->
            val productSummary = productSummaries.getValue(line.locationProductId)
            SaleLineDto(
                id = line.id!!,
                locationProductId = line.locationProductId,
                productLabel = productSummary.label,
                productPrintLabel = productSummary.printLabel,
                userProvidedQuantity = line.userProvidedQuantity,
                unitId = line.unitId,
                baseUnitId = productSummary.baseUnitId,
                storageUnitsPerUserProvidedUnit = line.storageUnitsPerUserProvidedUnit,
                lineTotal = line.lineTotal,
                defaultSalePrice = line.defaultSalePrice,
            )
        })
        return saleIds.associateWith { saleId -> saleLineDtosBySaleId[saleId].orEmpty() }
    }

    private fun loadHeaders(saleIds: List<UUID>): List<SaleHeaderDto> {
        val saleEntities = saleRepository.findAllById(saleIds)
        val missingSaleId = saleIds.firstOrNull { saleId -> saleEntities.none { it.id == saleId } }
        if (missingSaleId != null) throw RtsGenericException("Sale $missingSaleId not found")
        return saleEntities.map(::extractHeader)
    }

    private fun extractHeader(saleEntity: SaleEntity): SaleHeaderDto = SaleHeaderDto(
        id = saleEntity.id!!,
        referenceNumber = saleEntity.requiredReference(),
        version = saleEntity.version,
        status = saleEntity.status,
        contactId = saleEntity.contactId,
        soldById = saleEntity.soldById,
        dateSold = saleEntity.dateSold,
        notes = saleEntity.notes,
    )
}
