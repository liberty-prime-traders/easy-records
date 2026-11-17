package app.myliberty.easy_records.locations.business.sale

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SaleVoidRepository : JpaRepository<SaleVoidEntity, UUID> {
    fun findBySaleId(saleId: UUID): SaleVoidEntity?
}
