package app.myliberty.easy_records.cross_tier.core.authority

import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.util.enums.SchemaLevel
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
class AuthorizationRequestService(private val authorizationBatchExecutor: AuthorizationBatchExecutor) {

    fun assignAtPlatformTier(request: AuthorizationAssignmentRequest) {
        requireExactTier(request.allTiers, SchemaLevel.PLATFORM)
        authorizationBatchExecutor.assignAtPlatformTier(request)
    }

    fun removeAtPlatformTier(request: AuthorizationAssignmentRequest) {
        requireExactTier(request.allTiers, SchemaLevel.PLATFORM)
        authorizationBatchExecutor.removeAtPlatformTier(request)
    }

    fun assignAtOrganizationTier(request: AuthorizationAssignmentRequest) {
        requireExactTier(request.allTiers, SchemaLevel.ORGANIZATION)
        requireTierContext(SchemaLevel.ORGANIZATION)
        authorizationBatchExecutor.assignAtOrganizationTier(request)
    }

    fun removeAtOrganizationTier(request: AuthorizationAssignmentRequest) {
        requireExactTier(request.allTiers, SchemaLevel.ORGANIZATION)
        requireTierContext(SchemaLevel.ORGANIZATION)
        authorizationBatchExecutor.removeAtOrganizationTier(request)
    }

    fun assignAtLocationTier(request: AuthorizationAssignmentRequest) {
        requireExactTier(request.allTiers, SchemaLevel.LOCATION)
        requireTierContext(SchemaLevel.LOCATION)
        authorizationBatchExecutor.assignAtLocationTier(request)
    }

    fun removeAtLocationTier(request: AuthorizationAssignmentRequest) {
        requireExactTier(request.allTiers, SchemaLevel.LOCATION)
        requireTierContext(SchemaLevel.LOCATION)
        authorizationBatchExecutor.removeAtLocationTier(request)
    }

    private fun requireExactTier(tiers: List<SchemaLevel>, expected: SchemaLevel) {
        if (tiers.any { it != expected }) {
            throw RtsGenericException("This endpoint only accepts $expected-tier roles/permissions")
        }
    }

    private fun requireTierContext(tier: SchemaLevel) {
        when (tier) {
            SchemaLevel.ORGANIZATION -> if (SessionContextProvider.getOrganizationIdOrNull() == null) {
                throw RtsGenericException("Organization context is required for an organization-tier request")
            }
            SchemaLevel.LOCATION -> if (SessionContextProvider.getLocationIdOrNull() == null) {
                throw RtsGenericException("Location context is required for a location-tier request")
            }
            SchemaLevel.PLATFORM -> Unit
        }
    }
}
