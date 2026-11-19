package app.myliberty.easy_records.util.enums

import app.myliberty.easy_records.platform.business.startup_checks.api.StartupCheck
import org.springframework.stereotype.Component

@Component
class EsrNameConsistencyCheck : StartupCheck {

    override val name = "esr-role-and-permission-name-consistency"

    override fun check() {
        requireNamesMatch("EsrRoleNames", EsrRoleNames::class.java, EsrRole.entries.map { it.name })
        requireNamesMatch("EsrPermissionNames", EsrPermissionNames::class.java, EsrPermission.entries.map { it.name })
    }

    private fun requireNamesMatch(label: String, constantsClass: Class<*>, enumNames: List<String>) {
        val declaredNames = constantsClass.fields
            .filter { it.name != "INSTANCE" }
            .map { it.get(null) as String }
            .toSet()
        val enumNameSet = enumNames.toSet()
        if (declaredNames != enumNameSet) {
            throw IllegalStateException(
                "$label is out of sync with its enum - missing: ${enumNameSet - declaredNames}, extra: ${declaredNames - enumNameSet}"
            )
        }
    }
}
