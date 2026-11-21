package app.myliberty.easy_records.organizations.business.core.feature.activators

import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.util.exceptions.LibertyGenericException

interface FeatureActivator {
    val feature: Feature

    fun getUnmetPrerequisites(): Set<String> { return emptySet() }
    fun deactivate() {}
    fun onActivate() {}

    fun activate() {
        val unmetPrerequisites = getUnmetPrerequisites()
        if (unmetPrerequisites.isNotEmpty()) {
            throw LibertyGenericException("Cannot activate feature $feature", unmetPrerequisites)
        }
        onActivate()
    }
}
