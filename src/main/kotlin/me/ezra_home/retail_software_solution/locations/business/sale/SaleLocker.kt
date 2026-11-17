package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
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
