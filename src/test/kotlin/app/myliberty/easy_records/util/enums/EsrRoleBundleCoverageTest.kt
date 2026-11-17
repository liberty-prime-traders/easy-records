package app.myliberty.easy_records.util.enums

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class EsrRoleBundleCoverageTest {

    // Privilege-escalation-shaped permissions (granting further roles/permissions) are
    // deliberately kept add-on-only — bundling them into a general-purpose role would make
    // escalation implicit. Extend this set only for permissions with that same shape.
    private val bundleExemptPermissions = setOf(
        EsrPermission.MANAGE_ORGANIZATION_ACCESS,
        EsrPermission.MANAGE_LOCATION_ACCESS
    )

    @Test
    fun `every non-exempt EsrPermission is reachable through at least one EsrRole bundle`() {
        val bundledPermissions = EsrRole.entries.flatMap { it.permissions }.toSet()
        val unreachable = EsrPermission.entries.filterNot { it in bundledPermissions || it in bundleExemptPermissions }

        assertTrue(
            unreachable.isEmpty(),
            "These permissions can only ever be granted as add-ons, never via a role: $unreachable"
        )
    }
}
