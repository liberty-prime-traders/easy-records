package app.myliberty.easy_records.organizations.business.core.feature

import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class OrganizationFeatureStatusConverter : EnumConverter<OrganizationFeatureStatus>(OrganizationFeatureStatus::class.java)
