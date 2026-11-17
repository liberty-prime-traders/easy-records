package app.myliberty.easy_records.platform.business.reserved_subdomain

import jakarta.persistence.Converter
import app.myliberty.easy_records.platform.business.reserved_subdomain.api.ReservedDomainStatus
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class ReservedDomainStatusConverter : EnumConverter<ReservedDomainStatus>(ReservedDomainStatus::class.java)
