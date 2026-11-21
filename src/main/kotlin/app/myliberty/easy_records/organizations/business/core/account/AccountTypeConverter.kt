package app.myliberty.easy_records.organizations.business.core.account

import app.myliberty.easy_records.organizations.business.core.account.api.AccountType
import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class AccountTypeConverter : EnumConverter<AccountType>(AccountType::class.java)
