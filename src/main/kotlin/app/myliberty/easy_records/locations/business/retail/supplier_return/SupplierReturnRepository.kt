package app.myliberty.easy_records.locations.business.retail.supplier_return

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SupplierReturnRepository : JpaRepository<SupplierReturnEntity, UUID>
