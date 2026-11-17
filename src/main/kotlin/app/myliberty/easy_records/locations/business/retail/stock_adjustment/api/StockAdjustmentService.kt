package app.myliberty.easy_records.locations.business.stock_adjustment.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.location_product.api.LocationProductService
import app.myliberty.easy_records.locations.business.location_product.api.LocationProductUnitRequestDto
import app.myliberty.easy_records.locations.business.location_product.api.ProductStockBreakdownProvider
import app.myliberty.easy_records.locations.business.lock.api.EntityAdvisoryLock
import app.myliberty.easy_records.locations.business.stock.api.StockAdjustmentCategory
import app.myliberty.easy_records.locations.business.stock.api.updater.StockAdjustmentDecreaseStockRequest
import app.myliberty.easy_records.locations.business.stock.api.updater.StockAdjustmentIncreaseStockRequest
import app.myliberty.easy_records.locations.business.stock.api.StockAdjustmentMovementDetails
import app.myliberty.easy_records.locations.business.stock.api.updater.StockAdjustmentStockUpdater
import app.myliberty.easy_records.locations.business.stock_adjustment.StockAdjustmentCategoryRules
import app.myliberty.easy_records.locations.business.stock_adjustment.StockAdjustmentCostEntity
import app.myliberty.easy_records.locations.business.stock_adjustment.StockAdjustmentCostRepository
import app.myliberty.easy_records.locations.business.stock_adjustment.StockAdjustmentEntity
import app.myliberty.easy_records.locations.business.stock_adjustment.StockAdjustmentHandlerForKafka
import app.myliberty.easy_records.locations.business.stock_adjustment.StockAdjustmentRepository
import app.myliberty.easy_records.locations.business.stock_adjustment.StockAdjustmentValidator
import app.myliberty.easy_records.organizations.business.fiscal_period.api.FiscalPeriodService
import app.myliberty.easy_records.organizations.business.stock_movement_reason.api.StockMovementReasonService
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.Decimals
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import app.myliberty.easy_records.util.business.mappers.UserQualifier
import app.myliberty.easy_records.util.business.reference_number.ReferenceNumberGenerator
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.quantity.ExactQuantity
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnLocationSchema
class StockAdjustmentService(
    private val stockAdjustmentRepository: StockAdjustmentRepository,
    private val stockAdjustmentCostRepository: StockAdjustmentCostRepository,
    private val productStockBreakdownProvider: ProductStockBreakdownProvider,
    private val stockAdjustmentStockUpdater: StockAdjustmentStockUpdater,
    private val stockAdjustmentHandlerForKafka: StockAdjustmentHandlerForKafka,
    private val locationProductService: LocationProductService,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val stockMovementReasonService: StockMovementReasonService,
    private val fiscalPeriodService: FiscalPeriodService,
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val referenceNumberGenerator: ReferenceNumberGenerator,
    private val userQualifier: UserQualifier
) {

    fun adjust(stockAdjustmentCreateDto: StockAdjustmentCreateDto): StockAdjustmentResponseDto {
        val direction = StockAdjustmentValidator.guardCategoryRequest(stockAdjustmentCreateDto)
        val locationProductId = stockAdjustmentCreateDto.locationProductId
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, setOf(locationProductId))
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.Now.organization())
        locationProductService.guardAllActive(listOf(locationProductId))
        if (StockAdjustmentCategoryRules.requiresReason(stockAdjustmentCreateDto.category)) {
            stockMovementReasonService.guardReason(direction.movementType, stockAdjustmentCreateDto.stockMovementReasonId)
        }

        val storageUnitsPerUserProvidedUnit = locationProductDataFetcher.getStorageUnitsPerUserProvidedUnit(
            listOf(LocationProductUnitRequestDto(locationProductId, stockAdjustmentCreateDto.unitId))
        ).getValue(locationProductId)
        val storageQuantity = ExactQuantity.ofStorageFromUserProvided(stockAdjustmentCreateDto.userProvidedQuantity, storageUnitsPerUserProvidedUnit)
        val referenceNumber = referenceNumberGenerator.generateSingle(TableName.STOCK_ADJUSTMENT)
        val adjustmentDetails = StockAdjustmentMovementDetails(
            category = stockAdjustmentCreateDto.category,
            reasonId = stockAdjustmentCreateDto.stockMovementReasonId,
            note = StringUtils.getValueOrNull(stockAdjustmentCreateDto.note)
        )

        val productLabel = locationProductDataFetcher.findSummaryByIds(setOf(locationProductId)).getValue(locationProductId).printLabel

        val totalCost = when (direction) {
            StockAdjustmentDirection.INCREASE -> applyIncrease(
                stockAdjustmentCreateDto, storageQuantity, storageUnitsPerUserProvidedUnit, referenceNumber, adjustmentDetails
            )
            StockAdjustmentDirection.DECREASE -> stockAdjustmentStockUpdater.decreaseStock(
                StockAdjustmentDecreaseStockRequest(
                    locationProductId = locationProductId,
                    productLabel = productLabel,
                    storageQuantity = storageQuantity,
                    userProvidedQuantity = stockAdjustmentCreateDto.userProvidedQuantity.toBigDecimal(),
                    unitId = stockAdjustmentCreateDto.unitId,
                    storageUnitsPerUserProvidedUnit = storageUnitsPerUserProvidedUnit,
                    documentReference = referenceNumber,
                    adjustmentDetails = adjustmentDetails
                )
            ).totalCost
        }

        val savedStockAdjustment = stockAdjustmentRepository.save(
            StockAdjustmentEntity(
                locationProductId = locationProductId,
                category = stockAdjustmentCreateDto.category,
                direction = direction,
                userProvidedQuantity = stockAdjustmentCreateDto.userProvidedQuantity.toBigDecimal(),
                unitId = stockAdjustmentCreateDto.unitId,
                storageUnitsPerUserProvidedUnit = storageUnitsPerUserProvidedUnit,
                quantityText = productStockBreakdownProvider.forLocationProduct(locationProductId)
                    .renderUserProvided(stockAdjustmentCreateDto.userProvidedQuantity.toBigDecimal(), stockAdjustmentCreateDto.unitId),
                storageQuantity = storageQuantity.toStorageQuantity(),
                totalCost = totalCost,
                stockMovementReasonId = stockAdjustmentCreateDto.stockMovementReasonId,
                note = adjustmentDetails.note
            ).also { it.referenceNumber = referenceNumber }
        )
        stockAdjustmentCostRepository.save(
            StockAdjustmentCostEntity(stockAdjustmentId = savedStockAdjustment.id!!, totalCost = totalCost)
        )
        stockAdjustmentHandlerForKafka.publish(savedStockAdjustment)
        return toResponseDto(savedStockAdjustment, productLabel, totalCost)
    }

    @TransactionalOnLocationSchema(readOnly = true)
    fun getAll(): List<StockAdjustmentResponseDto> {
        val stockAdjustments = stockAdjustmentRepository.findAll()
        val activeTotalCostByStockAdjustmentId = stockAdjustmentCostRepository
            .findByStockAdjustmentIdInAndStatus(stockAdjustments.map { it.id!! }, StockAdjustmentCostStatus.ACTIVE)
            .associate { it.stockAdjustmentId to it.totalCost }
        val labelsByLocationProductId = locationProductDataFetcher
            .findSummaryByIds(stockAdjustments.map { stockAdjustment -> stockAdjustment.locationProductId }.toSet())
            .mapValues { locationProductSummary -> locationProductSummary.value.printLabel }
        return stockAdjustments.map { stockAdjustment ->
            toResponseDto(
                stockAdjustment,
                labelsByLocationProductId[stockAdjustment.locationProductId] ?: stockAdjustment.locationProductId.toString(),
                activeTotalCostByStockAdjustmentId.getValue(stockAdjustment.id!!)
            )
        }
    }

    @TransactionalOnLocationSchema(readOnly = true)
    fun getCategoryDescriptors(): List<StockAdjustmentCategoryDescriptor> {
        val reasonOptions = stockMovementReasonService.getAll().map { reason ->
            StockAdjustmentReasonOption(
                id = reason.id,
                name = reason.name,
                allowedDirections = StockAdjustmentDirection.entries.filter { direction -> direction.movementType in reason.movementTypes }
            )
        }.filter { reasonOption -> reasonOption.allowedDirections.isNotEmpty() }
        return StockAdjustmentCategory.entries.map { category ->
            StockAdjustmentCategoryDescriptor(
                category = category,
                displayName = StockAdjustmentCategoryRules.displayName(category),
                allowedDirections = StockAdjustmentCategoryRules.allowedDirections(category),
                unitCostDirections = StockAdjustmentCategoryRules.unitCostDirections(category),
                requiresReason = StockAdjustmentCategoryRules.requiresReason(category),
                requiresNote = StockAdjustmentCategoryRules.requiresNote(category),
                reasons = if (StockAdjustmentCategoryRules.requiresReason(category)) reasonOptions else emptyList()
            )
        }
    }

    private fun applyIncrease(
        stockAdjustmentCreateDto: StockAdjustmentCreateDto,
        storageQuantity: ExactQuantity,
        storageUnitsPerUserProvidedUnit: Long,
        referenceNumber: String,
        adjustmentDetails: StockAdjustmentMovementDetails
    ): BigDecimal {
        val totalCost = Decimals.MoneyScale.multiply(stockAdjustmentCreateDto.requiredUnitCost().toBigDecimal(), stockAdjustmentCreateDto.userProvidedQuantity.toBigDecimal())
        stockAdjustmentStockUpdater.increaseStock(
            StockAdjustmentIncreaseStockRequest(
                locationProductId = stockAdjustmentCreateDto.locationProductId,
                storageQuantity = storageQuantity,
                userProvidedQuantity = stockAdjustmentCreateDto.userProvidedQuantity.toBigDecimal(),
                unitId = stockAdjustmentCreateDto.unitId,
                storageUnitsPerUserProvidedUnit = storageUnitsPerUserProvidedUnit,
                totalCost = totalCost,
                documentReference = referenceNumber,
                adjustmentDetails = adjustmentDetails
            )
        )
        return totalCost
    }

    private fun toResponseDto(
        stockAdjustmentEntity: StockAdjustmentEntity,
        productLabel: String,
        activeTotalCost: BigDecimal
    ) = StockAdjustmentResponseDto(
        referenceNumber = stockAdjustmentEntity.requiredReference(),
        locationProductId = stockAdjustmentEntity.locationProductId,
        productLabel = productLabel,
        category = stockAdjustmentEntity.category,
        direction = stockAdjustmentEntity.direction,
        userProvidedQuantity = stockAdjustmentEntity.userProvidedQuantity,
        unitId = stockAdjustmentEntity.unitId,
        quantityText = stockAdjustmentEntity.quantityText,
        totalCost = activeTotalCost,
        stockMovementReasonId = stockAdjustmentEntity.stockMovementReasonId,
        note = stockAdjustmentEntity.note,
        adjustedBy = userQualifier.getCreatorFullName(stockAdjustmentEntity.requiredCreatedById()),
        adjustedAt = stockAdjustmentEntity.requiredCreatedOn()
    )
}
