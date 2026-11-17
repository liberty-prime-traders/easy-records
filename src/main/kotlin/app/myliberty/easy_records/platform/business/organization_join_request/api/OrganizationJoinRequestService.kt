package app.myliberty.easy_records.platform.business.organization_join_request.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnPlatformSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.organizations.business.core.organization_user.api.OrganizationUserService
import app.myliberty.easy_records.platform.business.organization.api.OrganizationDto
import app.myliberty.easy_records.platform.business.organization.api.OrganizationResponseDto
import app.myliberty.easy_records.platform.business.organization_join_request.OrganizationJoinRequestCache
import app.myliberty.easy_records.platform.business.organization_join_request.OrganizationJoinRequestMapper
import org.springframework.stereotype.Service
import java.util.Collections
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class OrganizationJoinRequestService(
    private val organizationJoinRequestCache: OrganizationJoinRequestCache,
    private val organizationJoinRequestMapper: OrganizationJoinRequestMapper,
    private val organizationUserService: OrganizationUserService
) {

    fun createJoinRequest(
        subdomain: String, userId: UUID, organization: OrganizationDto?
    ): OrganizationLaunchResponseDto {
        val hasPendingRequest = organizationJoinRequestCache.existsBySubdomainAndCreatedByIdAndStatus(
            subdomain, userId, JoinRequestStatus.PENDING
        )
        if (hasPendingRequest.not()) {
            organizationJoinRequestCache.create(
                OrganizationJoinRequestInsertDto(
                    subdomain = subdomain,
                    status = JoinRequestStatus.PENDING,
                    organizationId = organization?.id
                )
            )
        }
        return organizationJoinRequestMapper.toLaunchResponse(
            organization = null,
            isOrganizationAdmin = false,
            accessRequested = true
        )
    }

    fun buildMemberLaunchResponse(organization: OrganizationResponseDto, isOrganizationAdmin: Boolean): OrganizationLaunchResponseDto =
        organizationJoinRequestMapper.toLaunchResponse(organization, isOrganizationAdmin, false)

    fun getUserJoinRequests(): Collection<OrganizationJoinRequestResponseDto> =
        organizationJoinRequestCache.getUserJoinRequests(SessionContextProvider.getUserId())
            .map { organizationJoinRequestMapper.toDto(it) }

    fun getOrganizationJoinRequests(): Collection<OrganizationAdminJoinRequestResponseDto> =
        organizationJoinRequestCache.getOrganizationJoinRequests(SessionContextProvider.getOrganizationId())
            .map { organizationJoinRequestMapper.toAdminDto(it) }

    fun admitUsers(joinRequestIds: Collection<UUID>): Collection<OrganizationAdminJoinRequestResponseDto> {
        val joinRequestsResponse = updateJoinRequests(joinRequestIds, JoinRequestStatus.APPROVED)
        organizationUserService.admitJoinRequests(joinRequestsResponse)
        return joinRequestsResponse
    }

    fun denyUsers(joinRequestIds: Collection<UUID>): Collection<OrganizationAdminJoinRequestResponseDto> =
        updateJoinRequests(joinRequestIds, JoinRequestStatus.DENIED)

    private fun updateJoinRequests(
        joinRequestIds: Collection<UUID>,
        newStatus: JoinRequestStatus
    ): Collection<OrganizationAdminJoinRequestResponseDto> {
        if (joinRequestIds.isEmpty()) return Collections.emptyList()
        val updated = organizationJoinRequestCache.getOrganizationJoinRequests(SessionContextProvider.getOrganizationId())
            .filter { joinRequestIds.contains(it.id) && it.status == JoinRequestStatus.PENDING }
            .map { it.copy(status = newStatus) }
        val saved = organizationJoinRequestCache.saveAll(updated)
        return saved.map { organizationJoinRequestMapper.toAdminDto(it) }
    }
}
