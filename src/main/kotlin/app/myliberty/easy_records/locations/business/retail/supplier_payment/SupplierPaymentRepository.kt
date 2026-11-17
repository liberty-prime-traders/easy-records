package app.myliberty.easy_records.locations.business.retail.supplier_payment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SupplierPaymentRepository : JpaRepository<SupplierPaymentEntity, UUID> {
    fun findByPurchaseId(purchaseId: UUID): List<SupplierPaymentEntity>
}
