package app.myliberty.easy_records.locations.business.retail.sale

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.lock.api.EntityAdvisoryLock
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleStatus
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

@Component
class SaleLocker(
    private val saleRepository: SaleRepository,
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun lockAndGetSale(saleId: UUID): SaleEntity = lockAndGetSales(listOf(saleId)).single()

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun lockAndGetSales(saleIds: List<UUID>): List<SaleEntity> {
        saleIds.sorted().forEach { entityAdvisoryLock.acquire(LockNamespaces.SALE, it) }
        val saleEntities = saleRepository.findAllById(saleIds)
        val missingSaleId = saleIds.firstOrNull { saleId -> saleEntities.none { it.id == saleId } }
        if (missingSaleId != null) throw RtsGenericException("Sale $missingSaleId no longer exists")
        return saleEntities
    }

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun loadDraftAtVersion(saleId: UUID, expectedVersion: Long?): SaleEntity {
        val sale = lockAndGetSale(saleId)
        if (sale.status != SaleStatus.DRAFT) {
            throw RtsGenericException("Sale ${sale.requiredReference()} is not a draft")
        }
        val expected = expectedVersion
            ?: throw RtsGenericException("Expected version must be supplied when committing an existing sale")
        if (sale.version != expected) {
            throw RtsGenericException(
                "Sale ${sale.requiredReference()} has been modified since you opened it — please reload and try again"
            )
        }
        return sale
    }
}
