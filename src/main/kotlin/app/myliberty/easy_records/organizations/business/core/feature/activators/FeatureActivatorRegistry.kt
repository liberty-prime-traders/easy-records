package app.myliberty.easy_records.organizations.business.core.feature.activators

import app.myliberty.easy_records.platform.business.feature.api.Feature
import org.springframework.stereotype.Component

@Component
class FeatureActivatorRegistry(activators: List<FeatureActivator>) {
    private val handlers: Map<Feature, List<FeatureActivator>> =  activators.groupBy { it.feature }

    fun getHandler(feature: Feature): List<FeatureActivator> = handlers[feature] ?: emptyList()
}
