package app.myliberty.easy_records.platform.business.organization

import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.Objects
import java.util.Optional
import java.util.UUID

@Component
class OrganizationValidator(private val organizationCache: OrganizationCache) {

    fun validateNameOnSave(name: String, id: UUID? = null) {
        organizationCache.getAllOrganizations()
            .find { StringUtils.isEquivalent(it.name, name) && !Objects.equals(it.id, id) }
            ?.let { throw RtsGenericException("An organization using the name '$name' already exists") }
    }

    fun validateNameOnSave(optionalName: Optional<String>?, id: UUID? = null) {
        optionalName?.let {
            val name = StringUtils.getValueOrException(it, "Organization name cannot be empty")
            validateNameOnSave(name, id)
        }
    }
}
