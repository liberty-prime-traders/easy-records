package app.myliberty.easy_records.locations.business.retail.last_purchase_cost_backfill.api

import app.myliberty.easy_records.configuration.session.ServiceAccountContext
import app.myliberty.easy_records.configuration.session.withLocationSession
import app.myliberty.easy_records.locations.business.retail.last_purchase_cost_backfill.LocationLastPurchaseCostBackfiller
import app.myliberty.easy_records.locations.business.retail.last_purchase_cost_backfill.LastPurchaseCostBackfillLogFormatter
import app.myliberty.easy_records.locations.business.retail.last_purchase_cost_backfill.LastPurchaseCostBackfillResult
import app.myliberty.easy_records.organizations.business.core.location.api.LocationDto
import app.myliberty.easy_records.organizations.business.core.location.api.LocationService
import app.myliberty.easy_records.util.async.AsyncExecutor
import app.myliberty.easy_records.util.enums.ServiceAccount
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

// Short-lived: deleted with the rest of the last purchase cost backfill once every organization has run it.
@Service
class LastPurchaseCostBackfiller(
    private val locationService: LocationService,
    private val locationLastPurchaseCostBackfiller: LocationLastPurchaseCostBackfiller,
    private val asyncExecutor: AsyncExecutor,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun startBackfillAllLocations() {
        asyncExecutor.execute {
            ServiceAccountContext.runWithServiceAccount(ServiceAccount.RECORD_INITIALIZER) {
                backfillAllLocations()
            }
        }
    }

    private fun backfillAllLocations() {
        val locations = locationService.getAllLocationDtos()
        log.info("Last purchase cost backfill started for {} location(s)", locations.size)

        val results = mutableListOf<LastPurchaseCostBackfillResult>()
        val failedLocationNames = mutableListOf<String>()
        locations.forEach { location ->
            try {
                val result = backfillLocation(location)
                results.add(result)
                log.info(
                    "Last purchase cost backfill location finished\n{}",
                    LastPurchaseCostBackfillLogFormatter.formatLocation(result)
                )
            } catch (exception: Exception) {
                failedLocationNames.add(location.name!!)
                log.error(
                    "Last purchase cost backfill FAILED for location '{}'; " +
                        "batches already committed are kept, re-run to resume",
                    location.name, exception
                )
            }
        }

        log.info(
            "Last purchase cost backfill finished\n{}",
            LastPurchaseCostBackfillLogFormatter.formatSummary(results, failedLocationNames)
        )
    }

    private fun backfillLocation(location: LocationDto): LastPurchaseCostBackfillResult =
        withLocationSession(location) {
            var updatedProductCount = 0L
            var lastLocationProductId = UUID(0L, 0L)
            while (true) {
                val batch = locationLastPurchaseCostBackfiller.backfillNextBatch(lastLocationProductId) ?: break
                updatedProductCount += batch.updatedProductCount
                lastLocationProductId = batch.lastLocationProductId
            }
            LastPurchaseCostBackfillResult(
                locationId = location.id,
                locationName = location.name!!,
                updatedProductCount = updatedProductCount,
            )
        }
}
