package app.myliberty.easy_records.organizations.business.feature

import app.myliberty.easy_records.util.enums.HasCode

enum class OrganizationFeatureStatus(override val code: String) : HasCode {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE");
}
