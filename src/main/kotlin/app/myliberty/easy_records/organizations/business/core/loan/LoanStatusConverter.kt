package app.myliberty.easy_records.organizations.business.core.loan

import jakarta.persistence.Converter
import app.myliberty.easy_records.organizations.business.core.loan.api.LoanStatus
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class LoanStatusConverter : EnumConverter<LoanStatus>(LoanStatus::class.java)
