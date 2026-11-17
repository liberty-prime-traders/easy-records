package app.myliberty.easy_records.organizations.business.feature.activators

import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.util.exceptions.RtsGenericException

interface FeatureActivator {
    val feature: Feature

    fun getUnmetPrerequisites(): Set<String> { return emptySet() }
    fun deactivate() {}
    fun onActivate() {}

    fun activate() {
        val unmetPrerequisites = getUnmetPrerequisites()
        if (unmetPrerequisites.isNotEmpty()) {
            throw RtsGenericException("Cannot activate feature $feature", unmetPrerequisites)
        }
        onActivate()
    }
}
