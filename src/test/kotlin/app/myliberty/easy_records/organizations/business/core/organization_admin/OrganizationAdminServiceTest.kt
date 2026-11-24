package app.myliberty.easy_records.organizations.business.core.organization_admin

import app.myliberty.easy_records.organizations.business.core.organization_admin.api.OrganizationAdminService
import app.myliberty.easy_records.platform.business.sysuser.api.SysUserService
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.http.HttpStatus
import java.util.UUID

class OrganizationAdminServiceTest {

    private val sysUserService = mock(SysUserService::class.java)
    private val organizationAdminCache = mock(OrganizationAdminCache::class.java)
    private val organizationAdminService = OrganizationAdminService(
        mock(OrganizationAdminMapper::class.java),
        organizationAdminCache,
        sysUserService,
    )

    // The admin row is keyed by the user id, so an unknown user must be refused before any row is written.
    @Test
    fun `an admin cannot be created for a user who does not exist`() {
        val unknownUserId = UUID.randomUUID()

        val thrownException = assertThrows(LibertyGenericException::class.java) {
            organizationAdminService.createOrganizationAdmin(unknownUserId)
        }

        assertEquals("User with id $unknownUserId not found", thrownException.message)
        assertEquals(HttpStatus.BAD_REQUEST, thrownException.statusCode)
    }
}
