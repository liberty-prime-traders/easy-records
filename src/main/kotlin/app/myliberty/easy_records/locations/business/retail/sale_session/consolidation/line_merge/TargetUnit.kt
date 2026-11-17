package app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.line_merge

import java.util.UUID

data class TargetUnit(
    val unitId: UUID,
    val storageUnitsPerUnit: Long,
)
