package app.myliberty.easy_records.cucumber.support

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

// DateTimes reads the organization zone from the request session, which test threads do not have. This zone matches
// the default timezone of OrganizationInsertDto, which the test organization is created with, so the test's "today"
// is the same calendar day the server uses.
object TestDates {

    private val ORGANIZATION_ZONE: ZoneId = ZoneId.of("Africa/Addis_Ababa")

    private val DATABASE_ZONE: ZoneId = ZoneId.of("UTC")

    fun today(): LocalDate = LocalDate.now(ORGANIZATION_ZONE)

    // A timestamp read back from the database is in UTC, which is a different calendar day from the organization's
    // for three hours of every day.
    fun todayInDatabaseZone(): LocalDate = LocalDate.now(DATABASE_ZONE)

    fun now(): OffsetDateTime = OffsetDateTime.now(ORGANIZATION_ZONE)
}
