package app.myliberty.easy_records.organizations.business.core.organization_user

import app.myliberty.easy_records.organizations.business.core.organization_admin.api.OrganizationAdminService
import app.myliberty.easy_records.organizations.business.core.organization_user.api.OrganizationUserService
import app.myliberty.easy_records.organizations.business.core.permission_assignment.api.OrgPermissionAssignmentService
import app.myliberty.easy_records.organizations.business.core.role_assignment.api.OrgRoleAssignmentService
import app.myliberty.easy_records.platform.business.sysuser.api.SysUserService
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.http.HttpStatus

class OrganizationUserServiceTest {

    private val organizationUserService = OrganizationUserService(
        mock(OrganizationUserCache::class.java),
        mock(OrganizationAdminService::class.java),
        mock(OrganizationUserMapper::class.java),
        mock(OrgRoleAssignmentService::class.java),
        mock(OrgPermissionAssignmentService::class.java),
        mock(SysUserService::class.java),
    )

    // An empty batch would otherwise end no membership and look like a successful termination.
    @Test
    fun `terminating an empty list of users is refused with a bad request`() {
        val thrownException = assertThrows(LibertyGenericException::class.java) {
            organizationUserService.terminateOrganizationUsers(emptyList())
        }

        assertEquals("No users provided for termination", thrownException.message)
        assertEquals(HttpStatus.BAD_REQUEST, thrownException.statusCode)
    }
}
