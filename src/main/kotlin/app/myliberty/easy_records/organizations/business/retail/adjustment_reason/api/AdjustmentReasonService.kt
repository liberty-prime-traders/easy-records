package app.myliberty.easy_records.organizations.business.adjustment_reason.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.adjustment_reason.AdjustmentReasonCache
import app.myliberty.easy_records.organizations.business.adjustment_reason.AdjustmentReasonDto
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class AdjustmentReasonService(
    private val adjustmentReasonCache: AdjustmentReasonCache
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getById(id: UUID): AdjustmentReasonDto =
        adjustmentReasonCache.getAll().firstOrNull { it.id == id }
            ?: throw RtsGenericException("Adjustment reason $id does not exist")

    fun canApply(reason: AdjustmentReasonDto, direction: AdjustmentDirection): Boolean =
        reason.direction == AdjustmentDirection.BOTH || reason.direction == direction

    fun requireCanApply(reason: AdjustmentReasonDto, direction: AdjustmentDirection) {
        if (!canApply(reason, direction)) {
            throw RtsGenericException(
                "Adjustment reason '${reason.name}' cannot be applied as a ${direction.code} adjustment"
            )
        }
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun requireCanApply(reasonId: UUID, direction: AdjustmentDirection) {
        requireCanApply(getById(reasonId), direction)
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getReasonNamesById(): Map<UUID, String> =
        adjustmentReasonCache.getAll().associate { it.id to it.name }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getSystemReasonId(systemAdjustmentReason: SystemAdjustmentReason): UUID =
        adjustmentReasonCache.getAll().firstOrNull { it.code == systemAdjustmentReason.code }?.id
            ?: throw RtsGenericException("System adjustment reason ${systemAdjustmentReason.code} not seeded")

    fun create(dto: AdjustmentReasonInsertDto): AdjustmentReasonDto {
        if (dto.direction == AdjustmentDirection.BOTH) {
            throw RtsGenericException(
                "Direction BOTH is reserved for system-defined adjustment reasons"
            )
        }
        return adjustmentReasonCache.create(dto)
    }
}
