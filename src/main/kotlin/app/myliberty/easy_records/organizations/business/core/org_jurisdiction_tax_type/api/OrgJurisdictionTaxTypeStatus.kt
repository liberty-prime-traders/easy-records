package app.myliberty.easy_records.organizations.business.org_jurisdiction_tax_type.api

import app.myliberty.easy_records.util.enums.HasCode

enum class OrgJurisdictionTaxTypeStatus(override val code: String) : HasCode {
    ACTIVE("A"),
    INACTIVE("I")
}
