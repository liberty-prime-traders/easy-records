package app.myliberty.easy_records.organizations.business.core.liberty_access

import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.organizations.business.core.liberty_access.api.LibertyAccessService
import app.myliberty.easy_records.util.enums.EsrPermission
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LibertyAccessServiceTest {

    private val libertyAccessService = LibertyAccessService()

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    // Only the two liberty-level permissions leave the service; the org-user permissions bundled in roles stay private.
    @Test
    fun `only the liberty permissions held by the current user are returned`() {
        SessionContextProvider.setSession(
            SessionContext(
                permissions = setOf(
                    EsrPermission.MANAGE_ORGANIZATION_ACCESS,
                    EsrPermission.VIEW_USERS_OF_ORG,
                    EsrPermission.LIBERTY_ADMIN,
                    EsrPermission.REMOVE_USER_FROM_ORG,
                )
            )
        )

        val libertyPermissions = libertyAccessService.getLibertyPermissionsOfCurrentUser()

        assertEquals(
            setOf(EsrPermission.MANAGE_ORGANIZATION_ACCESS, EsrPermission.LIBERTY_ADMIN),
            libertyPermissions.toSet(),
        )
    }

    @Test
    fun `a user without liberty permissions gets an empty list`() {
        SessionContextProvider.setSession(
            SessionContext(permissions = setOf(EsrPermission.VIEW_USERS_OF_ORG))
        )

        assertEquals(emptyList<EsrPermission>(), libertyAccessService.getLibertyPermissionsOfCurrentUser())
    }
}
