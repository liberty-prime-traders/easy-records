package app.myliberty.easy_records.locations.business.purchase.api

import app.myliberty.easy_records.util.enums.PaymentStatus
import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.lock.api.EntityAdvisoryLock
import app.myliberty.easy_records.locations.business.purchase.PurchaseAssembler
import app.myliberty.easy_records.locations.business.purchase.PurchaseEntity
import app.myliberty.easy_records.locations.business.purchase.PurchaseLineRepository
import app.myliberty.easy_records.locations.business.purchase.PurchaseRepository
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class PurchaseDataFetcher(
  private val purchaseRepository: PurchaseRepository,
  private val purchaseLineRepository: PurchaseLineRepository,
  private val purchaseAssembler: PurchaseAssembler,
  private val entityAdvisoryLock: EntityAdvisoryLock
) {

  data class PurchaseInfo(
    val id: UUID,
    val referenceNumber: String,
    val supplierId: UUID,
    val purchaseStatus: PurchaseStatus,
    val paymentStatus: PaymentStatus
  ) {

    companion object {
      fun from(purchaseEntity: PurchaseEntity) = PurchaseInfo(
        purchaseEntity.id!!,
        purchaseEntity.requiredReference(),
        purchaseEntity.supplierId,
        purchaseEntity.purchaseStatus,
        purchaseEntity.paymentStatus
      )
    }
  }

  fun fetchTop(n: Int?): List<PurchaseResponseDto> {
    val recordCount = n ?: 10
    if (recordCount > 1000) throw RtsGenericException("Limit exceeds maximum of 1000")
    val sort = Sort.by(Sort.Direction.DESC, "createdOn")
    return purchaseAssembler.buildResponses(purchaseRepository.findTopN(PageRequest.of(0, recordCount, sort)))
  }

  fun findPurchaseInfoByReferenceNumber(referenceNumber: String): PurchaseInfo {
    val purchase = purchaseRepository.findByReferenceNumber(referenceNumber)
      ?: throw RtsGenericException("Purchase $referenceNumber not found")
    return PurchaseInfo.from(purchase)
  }

  fun getLocationProductIdOfLine(purchaseLineId: UUID): UUID =
    purchaseLineRepository.findLocationProductIdById(purchaseLineId)
      ?: throw RtsGenericException("Purchase line $purchaseLineId not found")

  fun getSupplierId(purchaseId: UUID): UUID {
    return purchaseRepository.getReferenceById(purchaseId).supplierId
  }

  fun findPurchaseInfoById(purchaseId: UUID): PurchaseInfo {
    return findPurchaseInfoByIds(setOf(purchaseId)).getValue(purchaseId)
  }

  fun findPurchaseInfoByIds(purchaseIds: Set<UUID>): Map<UUID, PurchaseInfo> {
    return purchaseRepository.findAllById(purchaseIds)
      .associateBy({ it.id!! }, { PurchaseInfo.from(it) })
  }

  @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
  fun lockPurchase(purchaseId: UUID) {
    entityAdvisoryLock.acquire(LockNamespaces.PURCHASE, purchaseId)
  }

  @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
  fun lockAndGetPurchase(purchaseId: UUID): PurchaseEntity {
    entityAdvisoryLock.acquire(LockNamespaces.PURCHASE, purchaseId)
    return purchaseRepository.getReferenceById(purchaseId)
  }

}
