package app.myliberty.easy_records.organizations.business.retail.unitgroup.api

import app.myliberty.easy_records.util.business.StringUtils
import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class UnitGroupUpdateDto(
    val id: UUID,
    val name: Optional<String>? = null,
    val description: Optional<String>? = null
) : Serializable {

    fun applyTo(existing: UnitGroupDto): UnitGroupDto = existing.copy(
        name = name?.orElse(existing.name) ?: existing.name,
        description = StringUtils.useIfProvided(description, existing.description)
    )
}
