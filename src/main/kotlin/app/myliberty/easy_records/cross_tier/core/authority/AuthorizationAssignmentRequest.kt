package app.myliberty.easy_records.cross_tier.core.authority

import app.myliberty.easy_records.util.enums.EsrPermission
import app.myliberty.easy_records.util.enums.EsrRole
import app.myliberty.easy_records.util.enums.SchemaLevel
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import java.util.UUID

// Batch: every user in userIds receives every role and every permission listed (cross product).
data class AuthorizationAssignmentRequest(
    val userIds: List<UUID>,
    val roles: List<EsrRole> = emptyList(),
    val permissions: List<EsrPermission> = emptyList()
) {
    init {
        if (userIds.isEmpty()) throw LibertyGenericException("At least one userId must be provided")
        if (roles.isEmpty() && permissions.isEmpty()) {
            throw LibertyGenericException("At least one role or permission must be provided")
        }
    }

    val allTiers: List<SchemaLevel> = roles.map { it.tier } + permissions.map { it.tier }
}
