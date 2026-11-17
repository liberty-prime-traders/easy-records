package app.myliberty.easy_records.locations.business.retail.sale_session.api

import java.util.UUID

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionUpdateFinalizer
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionStore
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionValidator
import app.myliberty.easy_records.locations.business.retail.sale_adjustment.api.PriceAdjustmentMethod
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
class SaleSessionAdjustmentHandler(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionValidator: SaleSessionValidator,
    private val saleSessionUpdateFinalizer: SaleSessionUpdateFinalizer,
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun add(sessionId: UUID, adjustmentAddDto: SaleSessionAdjustmentAddDto): SaleSessionResponseDto {
        val saleSession = saleSessionStore.load(sessionId)
        saleSessionValidator.guardAllowed(saleSession, addAction(adjustmentAddDto))
        saleSessionValidator.guardReasonIsNotPriceOverride(adjustmentAddDto.adjustmentReasonId)
        if (adjustmentAddDto.relatedSaleLineIdentity != null) {
            val targetLineKey = adjustmentAddDto.relatedSaleLineIdentity.key()
            if (saleSession.saleLines.none { it.identity.key() == targetLineKey }) {
                throw RtsGenericException("Adjustment references a line that is not on the sale")
            }
        }
        val newSaleSessionAdjustment = SaleSessionAdjustment(
            identity = SessionIdentity.mintFreshIdentity(),
            relatedSaleLineIdentity = adjustmentAddDto.relatedSaleLineIdentity,
            adjustmentReasonId = adjustmentAddDto.adjustmentReasonId,
            direction = adjustmentAddDto.direction,
            calculationMethod = PriceAdjustmentMethod.PERCENTAGE,
            value = adjustmentAddDto.percentage.toBigDecimal(),
            note = adjustmentAddDto.note,
            approvedById = adjustmentAddDto.approvedById,
        )
        val updatedSaleSession = saleSession.copy(saleAdjustments = saleSession.saleAdjustments + newSaleSessionAdjustment)
        return saleSessionUpdateFinalizer.finalize(updatedSaleSession)
    }

    private fun addAction(adjustmentAddDto: SaleSessionAdjustmentAddDto): SaleSessionAction =
        if (adjustmentAddDto.relatedSaleLineIdentity != null) {
            SaleSessionAction.ADD_LINE_ADJUSTMENT
        } else {
            SaleSessionAction.ADD_ORDER_LEVEL_ADJUSTMENT
        }

    fun remove(sessionId: UUID, rowIdentityDto: SaleSessionRowIdentityDto): SaleSessionResponseDto {
        val saleSession = saleSessionStore.load(sessionId)
        saleSessionValidator.guardAllowed(saleSession, SaleSessionAction.REMOVE_ADJUSTMENT)
        val targetAdjustmentKey = rowIdentityDto.identity.key()
        val survivingSaleAdjustments = saleSession.saleAdjustments.filter { it.identity.key() != targetAdjustmentKey }
        if (survivingSaleAdjustments.size == saleSession.saleAdjustments.size) {
            throw RtsGenericException("Adjustment not found on session")
        }
        val updatedSaleSession = saleSession.copy(saleAdjustments = survivingSaleAdjustments)
        return saleSessionUpdateFinalizer.finalize(updatedSaleSession)
    }

}
