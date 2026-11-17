package app.myliberty.easy_records.locations.business.sale_adjustment.api

import app.myliberty.easy_records.locations.business.sale_adjustment.SaleAdjustmentRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaleAdjustmentFetcher(
    private val saleAdjustmentRepository: SaleAdjustmentRepository
) {

    fun getAdjustmentSummaries(saleId: UUID): List<SaleAdjustmentSummaryDto> =
        saleAdjustmentRepository.findBySaleId(saleId).map { entity ->
            SaleAdjustmentSummaryDto(
                saleLineId = entity.saleLineId,
                direction = entity.direction,
                calculatedAmount = entity.calculatedAmount,
            )
        }

    fun getAdjustments(saleId: UUID): List<SaleAdjustmentDto> =
        getAdjustmentsBySaleId(listOf(saleId)).getValue(saleId)

    fun getAdjustmentsBySaleId(saleIds: List<UUID>): Map<UUID, List<SaleAdjustmentDto>> {
        val saleAdjustmentDtosBySaleId = saleAdjustmentRepository.findBySaleIdInOrderByCreatedOnAscIdAsc(saleIds)
            .groupBy({ it.saleId }, { entity ->
                SaleAdjustmentDto(
                    id = entity.id!!,
                    saleLineId = entity.saleLineId,
                    adjustmentReasonId = entity.adjustmentReasonId,
                    direction = entity.direction,
                    calculationMethod = entity.calculationMethod,
                    value = entity.value,
                    note = entity.note,
                    approvedById = entity.approvedById,
                )
            })
        return saleIds.associateWith { saleId -> saleAdjustmentDtosBySaleId[saleId].orEmpty() }
    }
}
