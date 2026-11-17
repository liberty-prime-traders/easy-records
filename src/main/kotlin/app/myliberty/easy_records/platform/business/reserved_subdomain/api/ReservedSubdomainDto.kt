package app.myliberty.easy_records.platform.business.reserved_subdomain.api

import java.io.Serializable
import java.util.UUID

data class ReservedSubdomainDto(
    val id: UUID,
    val subdomain: String?,
    val createdBy: String,
    val status: ReservedDomainStatus?,
    val referenceNumber: String?
): Serializable
