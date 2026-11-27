package app.myliberty.easy_records.cucumber.support

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

// DateTimes reads the organization zone from the request session, which test threads do not have. This zone matches
// the default timezone of OrganizationInsertDto, which the test organization is created with, so the test's "today"
// is the same calendar day the server uses.
object TestDates {

    private val ORGANIZATION_ZONE: ZoneId = ZoneId.of("Africa/Addis_Ababa")

    fun today(): LocalDate = LocalDate.now(ORGANIZATION_ZONE)

    fun now(): OffsetDateTime = OffsetDateTime.now(ORGANIZATION_ZONE)
}
