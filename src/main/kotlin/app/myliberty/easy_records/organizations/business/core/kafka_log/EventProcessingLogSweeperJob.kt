package app.myliberty.easy_records.organizations.business.core.kafka_log

import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.organizations.business.core.kafka_log.api.EventProcessingLogSweepService
import app.myliberty.easy_records.platform.business.organization.api.OrganizationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class EventProcessingLogSweeperJob(
    private val organizationService: OrganizationService,
    private val sweepService: EventProcessingLogSweepService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 */5 * * * *")
    fun run() {
        organizationService.getAllOrganizationDtos()
            .filter { it.schemaName != null }
            .forEach { org ->
                SessionContextProvider.initOrganization(org)
                try {
                    sweepService.reclaimStalePending(
                        sweepService.findStalePending(STALE_PENDING_AGE_MINUTES)
                    )
                    sweepService.retryAll(
                        sweepService.findRetryableToSweep(RETRYABLE_AGE_MINUTES)
                    )
                } catch (e: Exception) {
                    log.error("Sweep failed for org {}", org.id, e)
                } finally {
                    SessionContextProvider.clear()
                }
            }
    }

    companion object {
        private const val RETRYABLE_AGE_MINUTES = 1L
        private const val STALE_PENDING_AGE_MINUTES = 10L
    }
}
