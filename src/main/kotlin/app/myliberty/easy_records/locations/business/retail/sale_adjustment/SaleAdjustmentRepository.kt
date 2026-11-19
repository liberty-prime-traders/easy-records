package app.myliberty.easy_records.locations.business.retail.sale_adjustment

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SaleAdjustmentRepository : JpaRepository<SaleAdjustmentEntity, UUID> {
    fun findBySaleId(saleId: UUID): List<SaleAdjustmentEntity>
    fun findBySaleIdInOrderByCreatedOnAscIdAsc(saleIds: Collection<UUID>): List<SaleAdjustmentEntity>
}
