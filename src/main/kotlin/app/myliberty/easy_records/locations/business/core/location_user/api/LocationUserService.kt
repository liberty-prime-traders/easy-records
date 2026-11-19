package app.myliberty.easy_records.locations.business.core.location_user.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.cross_tier.core.authority.MembershipPeriod
import app.myliberty.easy_records.cross_tier.core.authority.MembershipSummaryRow
import app.myliberty.easy_records.cross_tier.core.authority.MembershipUserSummary
import app.myliberty.easy_records.cross_tier.core.authority.UserAccessDetail
import app.myliberty.easy_records.locations.business.core.location_user.LocationUserCache
import app.myliberty.easy_records.locations.business.core.location_user.LocationUserMapper
import app.myliberty.easy_records.locations.business.core.permission_assignment.api.LocationPermissionAssignmentService
import app.myliberty.easy_records.locations.business.core.role_assignment.api.LocationRoleAssignmentService
import app.myliberty.easy_records.platform.business.sysuser.api.SysUserService
import app.myliberty.easy_records.platform.business.sysuser.api.SysUserWithProfileDto
import app.myliberty.easy_records.util.enums.EsrPermission
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class LocationUserService(
    private val locationUserCache: LocationUserCache,
    private val locationUserMapper: LocationUserMapper,
    private val locationRoleAssignmentService: LocationRoleAssignmentService,
    private val locationPermissionAssignmentService: LocationPermissionAssignmentService,
    private val sysUserService: SysUserService
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun getLocationUserSummaries(): Collection<MembershipUserSummary> {
        val usersById = sysUserService.getUsersById()
        return locationUserCache.getMembershipSummaries()
            .map { row -> toMembershipUserSummary(row, usersById[row.userId]) }
    }

    private fun toMembershipUserSummary(
        membershipSummaryRow: MembershipSummaryRow,
        sysUserWithProfileDto: SysUserWithProfileDto?
    ) = MembershipUserSummary(
        userId = membershipSummaryRow.userId,
        fullName = sysUserWithProfileDto?.fullName,
        email = sysUserWithProfileDto?.email,
        membershipCount = membershipSummaryRow.membershipCount,
        active = membershipSummaryRow.active
    )

    @TransactionalOnLocationSchema(readOnly = true)
    fun getUserAccessDetail(userId: UUID): UserAccessDetail {
        val memberships = locationUserCache.getLocationUsers()
            .filter { it.userId == userId }
            .map { MembershipPeriod(it.id, it.createdOn, it.endOn) }
        return UserAccessDetail(
            userId = userId,
            memberships = memberships,
            roles = locationRoleAssignmentService.getGrantedRoles(userId),
            addOnPermissions = locationPermissionAssignmentService.getGrantedPermissions(userId)
        )
    }

    fun createLocationUsers(userIds: List<UUID>): Collection<MembershipUserSummary> {
        if (userIds.isEmpty()) {
            return emptyList()
        }
        val saved = locationUserCache.createAll(userIds.map { LocationUserInsertDto(userId = it) })
        val usersById = sysUserService.getUsersById()
        val membershipSummaryRowsByUserId = locationUserCache.getMembershipSummaries().associateBy { it.userId }
        return saved.map { locationUserDto ->
            toMembershipUserSummary(membershipSummaryRowsByUserId.getValue(locationUserDto.userId), usersById[locationUserDto.userId])
        }
    }

    fun registerFounder(userId: UUID) {
        val membership = locationUserCache.create(LocationUserInsertDto(userId = userId))
        val assignedById = SessionContextProvider.getUserId()
        locationPermissionAssignmentService.assignAll(
            mapOf(userId to membership.id),
            EsrPermission.MANAGE_LOCATION_ACCESS,
            assignedById
        )
    }

    fun terminateLocationUsers(locationUserIds: List<UUID>): Collection<LocationUserResponseDto> {
        if (locationUserIds.isEmpty()) {
            throw LibertyGenericException("No users provided for termination")
        }

        return locationUserCache.getLocationUsers().filter {
            locationUserIds.contains(it.userId) && it.isActive()
        }.map { dto ->
            val saved = locationUserCache.save(dto.copy(endOn = OffsetDateTime.now()))
            locationRoleAssignmentService.removeAllRoles(dto.userId)
            locationPermissionAssignmentService.removeAllPermissions(dto.userId)
            locationUserMapper.toDto(saved)
        }
    }

    @TransactionalOnLocationSchema(readOnly = true)
    fun getActiveMembershipIds(userIds: Collection<UUID>): Map<UUID, UUID> {
        val activeMembershipIdByUserId = locationUserCache.getLocationUsers()
            .filter { it.isActive() }
            .associate { it.userId to it.id }
        return userIds.associateWith { userId ->
            activeMembershipIdByUserId[userId]
                ?: throw LibertyGenericException("User is not an active member of this location")
        }
    }
}
