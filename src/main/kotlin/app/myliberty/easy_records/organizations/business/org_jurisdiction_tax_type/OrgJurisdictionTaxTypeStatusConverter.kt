package app.myliberty.easy_records.organizations.business.org_jurisdiction_tax_type

import jakarta.persistence.Converter
import app.myliberty.easy_records.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeStatus
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class OrgJurisdictionTaxTypeStatusConverter : EnumConverter<OrgJurisdictionTaxTypeStatus>(OrgJurisdictionTaxTypeStatus::class.java)
