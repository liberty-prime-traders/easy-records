package app.myliberty.easy_records.messaging.kafka.catalog

import app.myliberty.easy_records.configuration.session.ServiceAccountContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.retail.catalog_sync.api.SyncServiceRegistry
import app.myliberty.easy_records.messaging.kafka.common.EventSessionSetup
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.organizations.business.core.location.api.LocationService
import app.myliberty.easy_records.util.enums.ServiceAccount
import app.myliberty.easy_records.util.model.TableName
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class CatalogEventHandler(
    private val eventSessionSetup: EventSessionSetup,
    private val locationService: LocationService,
    private val syncServiceRegistry: SyncServiceRegistry,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    fun consume(event: CatalogChangedEvent) {
        ServiceAccountContext.runWithServiceAccount(ServiceAccount.CATALOG_SYNC) {
            eventSessionSetup.initFromEvent(event)

            locationService.getAllLocationDtos().forEach { location ->
                SessionContextProvider.initLocation(location)
                syncServiceRegistry.getService(event.tableName).syncSingle(event.entityId)
            }
        }
    }

    fun publish(tableName: TableName, entityId: UUID) {
        val orgSchema = SessionContextProvider.getOrganizationSchema()

        applicationEventPublisher.publishEvent(
            CatalogChangedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.OrgLevel(orgSchema),
                timestamp = Instant.now(),
                correlationId = null,
                tableName = tableName,
                entityId = entityId
            )
        )
    }
}
