package app.myliberty.easy_records.locations.business.sale_session.api

import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.sale.api.SaleSaveResult
import app.myliberty.easy_records.locations.business.sale.api.ConfirmedSalePersister
import app.myliberty.easy_records.locations.business.sale.api.DraftSalePersister
import app.myliberty.easy_records.locations.business.sale.api.SaleUpdater
import app.myliberty.easy_records.locations.business.sale.api.SaleVoidCreateDto
import app.myliberty.easy_records.locations.business.sale_session.SaleSessionAssembler
import app.myliberty.easy_records.locations.business.sale_session.SessionToSaveRequestMapper
import app.myliberty.easy_records.locations.business.sale_session.SaleSessionStore
import app.myliberty.easy_records.locations.business.sale_session.SaleSessionTotalsCalculator
import app.myliberty.easy_records.locations.business.sale_session.SaleSessionValidator
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.StringUtils
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaleSessionPersister(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionAssembler: SaleSessionAssembler,
    private val saleSessionValidator: SaleSessionValidator,
    private val sessionToSaveRequestMapper: SessionToSaveRequestMapper,
    private val draftSalePersister: DraftSalePersister,
    private val confirmedSalePersister: ConfirmedSalePersister,
    private val saleUpdater: SaleUpdater,
) {

    fun saveDraft(sessionId: UUID): SaleSessionResponseDto {
        val saleSession = loadAndValidate(sessionId, SaleSessionAction.SAVE_DRAFT)
        val saleSaveRequest = sessionToSaveRequestMapper.toSaleSaveRequest(saleSession)
        val saleSaveResult = draftSalePersister.saveDraft(saleSaveRequest)
        val saleSessionAfterSave = applySaleSaveResult(saleSession, saleSaveResult)
        val saleSessionWithTotals = SaleSessionTotalsCalculator.recompute(saleSessionAfterSave)
        saleSessionStore.save(saleSessionWithTotals)
        return saleSessionAssembler.buildResponse(saleSessionWithTotals)
    }

    fun confirm(sessionId: UUID): SaleSessionResponseDto {
        val saleSession = loadAndValidate(sessionId, SaleSessionAction.CONFIRM)
        saleSessionValidator.guardNonEmptyLines(saleSession)
        saleSessionValidator.guardWalkInFullyCovered(saleSession)
        saleSessionValidator.guardPaymentsWithinTotal(saleSession)
        val saleSaveRequest = sessionToSaveRequestMapper.toSaleSaveRequest(saleSession)
        val saleSaveResult = confirmedSalePersister.confirm(saleSaveRequest)
        val saleSessionAfterSave = applySaleSaveResult(saleSession, saleSaveResult)
        val saleSessionResponseDto = saleSessionAssembler.buildResponse(saleSessionAfterSave)
        saleSessionStore.delete(sessionId)
        return saleSessionResponseDto
    }

    fun voidSale(sessionId: UUID, saleSessionVoidDto: SaleSessionVoidDto): SaleSessionResponseDto {
        val saleSession = saleSessionStore.load(sessionId)
        saleSessionValidator.guardAllowed(saleSession, SaleSessionAction.VOID_SALE)
        val saleId = saleSession.saleId
        if (saleId == null) {
            saleSessionStore.delete(sessionId)
            return saleSessionAssembler.buildResponse(saleSession)
        }
        val saleSummary = saleUpdater.voidSale(SaleVoidCreateDto(saleId, StringUtils.normalize(saleSessionVoidDto.reason)))
        val now = DateTimes.Offset.Now.organization()
        val saleSessionAfterVoid = saleSession.copy(
            originalStatus = saleSummary.status,
            lastUpdatedAt = now,
            lastAccessedById = SessionContextProvider.getUserId(),
            lastAccessedAt = now,
        )
        val saleSessionResponseDto = saleSessionAssembler.buildResponse(saleSessionAfterVoid)
        saleSessionStore.delete(sessionId)
        return saleSessionResponseDto
    }

    private fun loadAndValidate(sessionId: UUID, action: SaleSessionAction): SaleSession {
        val saleSession = saleSessionStore.load(sessionId)
        saleSessionValidator.guardAllowed(saleSession, action)
        val saleSessionWithTotals = SaleSessionTotalsCalculator.recompute(saleSession)
        saleSessionValidator.validate(saleSessionWithTotals)
        saleSessionValidator.guardNoInactiveProducts(saleSessionWithTotals)
        return saleSessionWithTotals
    }

    private fun applySaleSaveResult(saleSession: SaleSession, saleSaveResult: SaleSaveResult): SaleSession {
        val now = DateTimes.Offset.Now.organization()
        val userId = SessionContextProvider.getUserId()
        return saleSession.copy(
            saleId = saleSaveResult.saleId,
            saleVersion = saleSaveResult.newVersion,
            productsReservedAtVersion = saleSession.productsVersion,
            originalStatus = saleSaveResult.saleStatus,
            lastUpdatedAt = now,
            lastAccessedAt = now,
            lastAccessedById = userId,
            header = saleSession.header.copy(
                referenceNumber = saleSaveResult.saleReferenceNumber,
                dateSold = saleSaveResult.dateSold,
                soldById = saleSaveResult.soldById,
            ),
            saleLines = saleSession.saleLines.map { saleSessionLine ->
                val newSaleLineId = saleSaveResult.saleLineIdsByClientKey[saleSessionLine.identity.key()]
                if (newSaleLineId != null && !saleSessionLine.identity.isPersisted()) {
                    saleSessionLine.copy(identity = SessionIdentity.persisted(newSaleLineId))
                } else saleSessionLine
            },
            saleAdjustments = saleSession.saleAdjustments.map { saleSessionAdjustment ->
                val newSaleAdjustmentId = saleSaveResult.saleAdjustmentIdsByClientKey[saleSessionAdjustment.identity.key()]
                val updatedIdentity = if (newSaleAdjustmentId != null && !saleSessionAdjustment.identity.isPersisted()) {
                    SessionIdentity.persisted(newSaleAdjustmentId)
                } else saleSessionAdjustment.identity
                val updatedRelatedSaleLineIdentity = saleSessionAdjustment.relatedSaleLineIdentity?.let { relatedSaleLineIdentity ->
                    val newSaleLineId = saleSaveResult.saleLineIdsByClientKey[relatedSaleLineIdentity.key()]
                    if (newSaleLineId != null && !relatedSaleLineIdentity.isPersisted()) {
                        SessionIdentity.persisted(newSaleLineId)
                    } else relatedSaleLineIdentity
                }
                saleSessionAdjustment.copy(identity = updatedIdentity, relatedSaleLineIdentity = updatedRelatedSaleLineIdentity)
            },
            salePayments = saleSession.salePayments.map { saleSessionPayment ->
                val persistedSalePayment = saleSaveResult.persistedSalePaymentsByClientKey[saleSessionPayment.identity.key()]
                if (persistedSalePayment != null && !saleSessionPayment.identity.isPersisted()) {
                    saleSessionPayment.copy(
                        identity = SessionIdentity.persisted(persistedSalePayment.id),
                        paymentDate = persistedSalePayment.paymentDate,
                    )
                } else saleSessionPayment
            },
        )
    }
}
