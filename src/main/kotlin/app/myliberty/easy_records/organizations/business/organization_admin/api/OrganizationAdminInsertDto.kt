package app.myliberty.easy_records.organizations.business.organization_admin.api

import java.io.Serializable
import java.util.UUID

data class OrganizationAdminInsertDto(
    val userId: UUID
) : Serializable
