package app.myliberty.easy_records.organizations.business.retail.stock_movement_reason

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StockMovementReasonRepository : JpaRepository<StockMovementReasonEntity, UUID>
