package app.myliberty.easy_records.platform.business.organization_join_request

import app.myliberty.easy_records.platform.business.organization_join_request.api.JoinRequestStatus
import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationJoinRequestDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val subdomain: String,
    val status: JoinRequestStatus,
    val organizationId: UUID?
)
