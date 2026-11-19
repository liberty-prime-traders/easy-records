package app.myliberty.easy_records.organizations.business.core.jobtitle.api

import app.myliberty.easy_records.organizations.business.core.jobtitle.JobTitleDto
import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class JobTitleUpdateDto(
    val id: UUID? = null,
    val value: Optional<String>? = null,
) : Serializable {

    fun applyTo(existing: JobTitleDto): JobTitleDto = existing.copy(
        value = value?.orElse(existing.value) ?: existing.value
    )
}
