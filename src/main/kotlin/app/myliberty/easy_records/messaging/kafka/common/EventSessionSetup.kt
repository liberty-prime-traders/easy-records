package app.myliberty.easy_records.messaging.kafka.common

import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.organizations.business.location.api.LocationService
import app.myliberty.easy_records.platform.business.organization.api.OrganizationService
import org.springframework.stereotype.Component

@Component
class EventSessionSetup(
    private val organizationService: OrganizationService,
    private val locationService: LocationService
) {

    fun initFromEvent(event: BaseEvent) {
        when (val ctx = event.sourceContext) {
            is EventSourceContext.OrgLevel -> {
                SessionContextProvider.initOrganization(organizationService.getBySchema(ctx.orgSchema))
            }
            is EventSourceContext.LocationLevel -> {
                SessionContextProvider.initOrganization(organizationService.getBySchema(ctx.orgSchema))
                SessionContextProvider.initLocation(locationService.getBySchema(ctx.locationSchema))
            }
        }
    }
}
