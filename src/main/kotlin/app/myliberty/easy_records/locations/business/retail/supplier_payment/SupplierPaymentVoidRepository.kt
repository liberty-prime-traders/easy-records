package app.myliberty.easy_records.locations.business.retail.supplier_payment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SupplierPaymentVoidRepository : JpaRepository<SupplierPaymentVoidEntity, UUID> {
    fun existsBySupplierPaymentId(supplierPaymentId: UUID): Boolean
    fun findBySupplierPaymentIdIn(supplierPaymentIds: Collection<UUID>): List<SupplierPaymentVoidEntity>
}
