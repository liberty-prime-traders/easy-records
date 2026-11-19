package app.myliberty.easy_records.platform.business.organization_join_request.api

import java.io.Serializable
import java.util.UUID

data class OrganizationJoinRequestInsertDto(
    val subdomain: String,
    val status: JoinRequestStatus,
    val organizationId: UUID? = null
) : Serializable
