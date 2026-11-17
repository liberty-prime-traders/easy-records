package app.myliberty.easy_records.platform.business.feature.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnPlatformSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.organizations.business.core.feature.api.OrganizationFeatureService
import app.myliberty.easy_records.platform.business.feature.FeatureCache
import app.myliberty.easy_records.platform.business.organization.api.OrganizationService
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema(readOnly = true)
class FeatureService(
    private val featureCache: FeatureCache,
    private val organizationService: OrganizationService,
    private val organizationFeatureService: OrganizationFeatureService
) {

    fun getAll(): Collection<FeatureDto> = featureCache.getAll()

    fun activate(organizationId: UUID, features: HashSet<Feature>) {
        val org = organizationService.getAllOrganizationDtos().find { it.id == organizationId }
            ?: throw RtsGenericException("Organization $organizationId not found")
        SessionContextProvider.initOrganization(org)
        organizationFeatureService.activate(features)
    }

    fun deactivate(organizationId: UUID, features: HashSet<Feature>) {
        val org = organizationService.getAllOrganizationDtos().find { it.id == organizationId }
            ?: throw RtsGenericException("Organization $organizationId not found")
        SessionContextProvider.initOrganization(org)
        organizationFeatureService.deactivate(features)
    }

    @TransactionalOnPlatformSchema
    fun updateDescription(feature: FeatureUpdateDto): FeatureDto {
        val existing = featureCache.getAll().find { it.feature == feature.feature }
            ?: throw RtsGenericException("Feature ${feature.feature} not found")
        val updated = feature.applyTo(existing)
        featureCache.update(updated)
        return updated
    }
}
