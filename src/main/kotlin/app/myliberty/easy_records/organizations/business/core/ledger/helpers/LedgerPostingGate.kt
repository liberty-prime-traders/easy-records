package app.myliberty.easy_records.organizations.business.core.ledger.helpers

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.organizations.business.core.ledger.LedgerEntryGroupRepository
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerSourceType
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Component

@Component
class LedgerPostingGate(
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository
) {

    fun isPosted(sourceContext: EventSourceContext, reference: String, sourceType: LedgerSourceType): Boolean =
        when (sourceContext) {
            is EventSourceContext.OrgLevel ->
                ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceTypeAndSourceLocationIdIsNull(reference, sourceType)
            is EventSourceContext.LocationLevel ->
                ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceTypeAndSourceLocationId(
                    reference, sourceType, requireSessionLocationId(sourceContext)
                )
        }

    // A void must fail loudly rather than be skipped: a skipped void is never revisited once the original posts on retry.
    @TransactionalOnOrganizationSchema(readOnly = true)
    fun requirePosted(sourceContext: EventSourceContext, reference: String, originalSourceType: LedgerSourceType) {
        if (!isPosted(sourceContext, reference, originalSourceType)) {
            throw LibertyGenericException("Cannot post the void of $reference: its ${originalSourceType.name} posting does not exist")
        }
    }

    // Read in the session the consumer initialised from the event, so the session tier is the event's tier.
    fun idempotencyConstraintName(): String =
        if (SessionContextProvider.getLocationIdOrNull() == null) "uq_ledger_entry_grp_ref_type_org" else "uq_ledger_entry_grp_ref_type_loc"

    // A mismatched session would key the existence check to another location and let a duplicate posting through.
    private fun requireSessionLocationId(locationLevel: EventSourceContext.LocationLevel) =
        SessionContextProvider.getLocationIdOrNull()
            ?.takeIf { SessionContextProvider.getLocationSchema() == locationLevel.locationSchema }
            ?: throw LibertyGenericException("Location-level event for ${locationLevel.locationSchema} processed outside that location's session")
}
