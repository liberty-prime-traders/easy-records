package app.myliberty.easy_records.locations.business.retail.sale

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SaleRepository : JpaRepository<SaleEntity, UUID> {

    fun findByReferenceNumber(referenceNumber: String): SaleEntity?
}
