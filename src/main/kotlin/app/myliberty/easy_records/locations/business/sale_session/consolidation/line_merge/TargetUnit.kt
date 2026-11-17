package me.ezra_home.retail_software_solution.locations.business.sale_session.consolidation.line_merge

import java.util.UUID

data class TargetUnit(
    val unitId: UUID,
    val storageUnitsPerUnit: Long,
)
