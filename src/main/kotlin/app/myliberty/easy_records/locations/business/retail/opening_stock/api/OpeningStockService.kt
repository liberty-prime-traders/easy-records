package app.myliberty.easy_records.locations.business.retail.opening_stock.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductService
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductUnitRequestDto
import app.myliberty.easy_records.locations.business.core.lock.api.EntityAdvisoryLock
import app.myliberty.easy_records.locations.business.retail.opening_stock.OpeningStockEntity
import app.myliberty.easy_records.locations.business.retail.opening_stock.OpeningStockHandlerForKafka
import app.myliberty.easy_records.locations.business.retail.opening_stock.OpeningStockRepository
import app.myliberty.easy_records.locations.business.retail.opening_stock.OpeningStockValidator
import app.myliberty.easy_records.locations.business.retail.stock.api.updater.OpeningStockLineStockRequest
import app.myliberty.easy_records.locations.business.retail.stock.api.updater.OpeningStockStockUpdater
import app.myliberty.easy_records.organizations.business.core.fiscal_period.api.FiscalPeriodService
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.Decimals
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import app.myliberty.easy_records.util.business.mappers.UserQualifier
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnLocationSchema
class OpeningStockService(
    private val openingStockRepository: OpeningStockRepository,
    private val openingStockStockUpdater: OpeningStockStockUpdater,
    private val locationProductService: LocationProductService,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val fiscalPeriodService: FiscalPeriodService,
    private val openingStockHandlerForKafka: OpeningStockHandlerForKafka,
    private val userQualifier: UserQualifier
) {

    fun declareInitialStock(lines: List<OpeningStockLineDto>): List<OpeningStockResponseDto> {
        OpeningStockValidator.guardNotEmpty(lines)
        OpeningStockValidator.guardNoDuplicateProducts(lines)

        val productIds = lines.map { it.locationProductId }
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.Now.organization())
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, productIds)

        val labelsByProductId = locationProductDataFetcher.findSummaryByIds(productIds).mapValues { it.value.label }
        locationProductService.guardAllActive(productIds)

        val alreadyDeclared = openingStockRepository.findByLocationProductIdIn(productIds)
            .map { it.locationProductId }.toSet()
        OpeningStockValidator.guardNotAlreadyDeclared(alreadyDeclared, productIds, labelsByProductId)

        val storageUnitsByUnitRequest = locationProductDataFetcher.getStorageUnitsByRequest(
            lines.flatMap { line ->
                listOf(
                    LocationProductUnitRequestDto(line.locationProductId, line.unitId),
                    LocationProductUnitRequestDto(line.locationProductId, line.resolvedCostUnitId)
                )
            }.distinct()
        )
        val savedOpeningStocks = openingStockRepository.saveAll(
            lines.map { line ->
                val storageUnitsPerUserProvidedUnit =
                    storageUnitsByUnitRequest.getValue(LocationProductUnitRequestDto(line.locationProductId, line.unitId))
                val storageUnitsPerCostUnit = storageUnitsByUnitRequest.getValue(
                    LocationProductUnitRequestDto(line.locationProductId, line.resolvedCostUnitId)
                )
                OpeningStockEntity(
                    locationProductId = line.locationProductId,
                    userProvidedQuantity = line.userProvidedQuantity.toBigDecimal(),
                    unitId = line.unitId,
                    storageUnitsPerUserProvidedUnit = storageUnitsPerUserProvidedUnit,
                    totalCost = toTotalCost(line, storageUnitsPerUserProvidedUnit, storageUnitsPerCostUnit)
                )
            }
        )
        val savedByProductId = savedOpeningStocks.associateBy { it.locationProductId }

        openingStockStockUpdater.recordOpeningStock(
            lines.map { line ->
                val openingStock = savedByProductId.getValue(line.locationProductId)
                OpeningStockLineStockRequest(
                    locationProductId = line.locationProductId,
                    userProvidedQuantity = line.userProvidedQuantity,
                    unitId = openingStock.unitId,
                    storageUnitsPerUserProvidedUnit = openingStock.storageUnitsPerUserProvidedUnit,
                    batchCost = openingStock.totalCost,
                    documentReference = openingStock.requiredReference()
                )
            }
        )

        savedOpeningStocks.forEach { openingStock -> openingStockHandlerForKafka.publish(openingStock) }
        return savedOpeningStocks.map { toResponseDto(it, labelsByProductId.getValue(it.locationProductId)) }
    }

    /** [OpeningStockLineDto.unitCost] is per cost unit, so the quantity goes to storage units first and the
     * division rounds once; converting the cost into the quantity unit would round at scale 4. */
    private fun toTotalCost(line: OpeningStockLineDto, storageUnitsPerUserProvidedUnit: Long, storageUnitsPerCostUnit: Long) =
        Decimals.MoneyScale.divide(
            line.userProvidedQuantity.toBigDecimal().multiply(BigDecimal(storageUnitsPerUserProvidedUnit)).multiply(line.unitCost.toBigDecimal()),
            BigDecimal(storageUnitsPerCostUnit)
        )

    @TransactionalOnLocationSchema(readOnly = true)
    fun getAll(): List<OpeningStockResponseDto> {
        val all = openingStockRepository.findAll()
        val labelsByProductId = locationProductDataFetcher.findSummaryByIds(all.map { it.locationProductId }).mapValues { it.value.label }
        return all.map { toResponseDto(it, labelsByProductId[it.locationProductId] ?: it.locationProductId.toString()) }
    }

    private fun toResponseDto(openingStock: OpeningStockEntity, productLabel: String) = OpeningStockResponseDto(
        referenceNumber = openingStock.requiredReference(),
        locationProductId = openingStock.locationProductId,
        productLabel = productLabel,
        userProvidedQuantity = openingStock.userProvidedQuantity,
        unitId = openingStock.unitId,
        unitCost = openingStock.unitCost(),
        declaredBy = userQualifier.getCreatorFullName(openingStock.requiredCreatedById()),
        declaredAt = openingStock.requiredCreatedOn()
    )
}
