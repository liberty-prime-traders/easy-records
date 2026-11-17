package app.myliberty.easy_records.organizations.business.core.organization_user.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.cross_tier.core.authority.MembershipPeriod
import app.myliberty.easy_records.cross_tier.core.authority.MembershipUserSummary
import app.myliberty.easy_records.cross_tier.core.authority.UserAccessDetail
import app.myliberty.easy_records.organizations.business.core.organization_admin.api.OrganizationAdminService
import app.myliberty.easy_records.organizations.business.core.organization_user.OrganizationUserCache
import app.myliberty.easy_records.organizations.business.core.organization_user.OrganizationUserMapper
import app.myliberty.easy_records.organizations.business.core.permission_assignment.api.OrgPermissionAssignmentService
import app.myliberty.easy_records.organizations.business.core.role_assignment.api.OrgRoleAssignmentService
import app.myliberty.easy_records.platform.business.organization_join_request.api.OrganizationAdminJoinRequestResponseDto
import app.myliberty.easy_records.platform.business.sysuser.api.SysUserService
import app.myliberty.easy_records.util.enums.EsrPermission
import app.myliberty.easy_records.util.enums.EsrRole
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class OrganizationUserService(
    private val organizationUserCache: OrganizationUserCache,
    private val organizationAdminService: OrganizationAdminService,
    private val organizationUserMapper: OrganizationUserMapper,
    private val orgRoleAssignmentService: OrgRoleAssignmentService,
    private val orgPermissionAssignmentService: OrgPermissionAssignmentService,
    private val sysUserService: SysUserService
) {
    @TransactionalOnOrganizationSchema(readOnly = true)
    fun isOrganizationMember(userId: UUID): Boolean {
        return organizationAdminService.isOrganizationAdmin()
                || organizationUserCache.getOrganizationUsers().any { it.userId == userId }
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getOrganizationUserSummaries(): Collection<MembershipUserSummary> {
        val usersById = sysUserService.getUsersById()
        return organizationUserCache.getMembershipSummaries()
            .map { row ->
                val user = usersById[row.userId]
                MembershipUserSummary(
                    userId = row.userId,
                    fullName = user?.fullName,
                    email = user?.email,
                    membershipCount = row.membershipCount,
                    active = row.active
                )
            }
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getUserAccessDetail(userId: UUID): UserAccessDetail {
        val memberships = organizationUserCache.getOrganizationUsers()
            .filter { it.userId == userId }
            .map { MembershipPeriod(it.id, it.createdOn, it.endOn) }
        return UserAccessDetail(
            userId = userId,
            memberships = memberships,
            roles = orgRoleAssignmentService.getGrantedRoles(userId),
            addOnPermissions = orgPermissionAssignmentService.getGrantedPermissions(userId)
        )
    }

    fun terminateOrganizationUsers(organizationUserIds: List<UUID>): Collection<OrganizationUserResponseDto> {
        if (organizationUserIds.isEmpty()) {
            throw LibertyGenericException("No users provided for termination")
        }

        return organizationUserCache.getOrganizationUsers().filter {
            organizationUserIds.contains(it.userId) && it.isActive()
        }.map { dto ->
            val saved = organizationUserCache.save(dto.copy(endOn = OffsetDateTime.now()))
            orgRoleAssignmentService.removeAllRoles(dto.userId)
            orgPermissionAssignmentService.removeAllPermissions(dto.userId)
            organizationUserMapper.toDto(saved)
        }
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getActiveMembershipIds(userIds: Collection<UUID>): Map<UUID, UUID> {
        val activeMembershipIdByUserId = organizationUserCache.getOrganizationUsers()
            .filter { it.isActive() }
            .associate { it.userId to it.id }
        return userIds.associateWith { userId ->
            activeMembershipIdByUserId[userId]
                ?: throw LibertyGenericException("User is not an active member of this organization")
        }
    }

    fun admitJoinRequests(joinRequests: Collection<OrganizationAdminJoinRequestResponseDto>) {
        joinRequests.map { OrganizationUserInsertDto(joinRequestId = it.id, userId = it.createdById) }
            .let { organizationUserCache.createAll(it) }
    }

    fun registerFounder(userId: UUID) {
        val membership = organizationUserCache.create(OrganizationUserInsertDto(userId = userId))
        val assignedById = SessionContextProvider.getUserId()
        val membershipIdByUserId = mapOf(userId to membership.id)
        orgRoleAssignmentService.assignAll(membershipIdByUserId, EsrRole.MANAGE_ORGANIZATION_USERS, assignedById)
        orgPermissionAssignmentService.assignAll(membershipIdByUserId, EsrPermission.MANAGE_ORGANIZATION_ACCESS, assignedById)
    }
}
