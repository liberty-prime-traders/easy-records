package app.myliberty.easy_records.messaging.kafka.catalog

import app.myliberty.easy_records.configuration.session.ServiceAccountContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.catalog_sync.api.SyncServiceRegistry
import app.myliberty.easy_records.messaging.kafka.common.EventSessionSetup
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.organizations.business.location.api.LocationService
import app.myliberty.easy_records.util.enums.ServiceAccount
import app.myliberty.easy_records.util.model.TableName
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class CatalogEventHandler(
    private val eventSessionSetup: EventSessionSetup,
    private val locationService: LocationService,
    private val syncServiceRegistry: SyncServiceRegistry,
    private val catalogEventProducer: CatalogEventProducer
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

        catalogEventProducer.publish(
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
