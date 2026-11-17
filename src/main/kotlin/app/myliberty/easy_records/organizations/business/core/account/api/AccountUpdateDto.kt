package app.myliberty.easy_records.organizations.business.core.account.api

import app.myliberty.easy_records.organizations.business.core.account.AccountDto
import java.util.Optional
import java.util.UUID

data class AccountUpdateDto(
    val id: UUID,
    val name: Optional<String>
) {

    fun applyTo(existing: AccountDto): AccountDto {
        return existing.copy(
            name = name.orElseGet { existing.name }
        )
    }
}
