package app.myliberty.easy_records.organizations.business.retail.adjustment_reason

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AdjustmentReasonRepository : JpaRepository<AdjustmentReasonEntity, UUID>
