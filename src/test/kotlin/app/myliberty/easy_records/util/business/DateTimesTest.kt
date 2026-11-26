package app.myliberty.easy_records.util.business

import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class DateTimesTest {

    private val organizationToday: LocalDate = LocalDate.now(ZoneId.of("UTC"))

    @BeforeEach
    fun setUp() {
        SessionContextProvider.setSession(
            SessionContext(organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"))
        )
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `a range that starts and ends today contains today`() {
        assertTrue(DateTimes.todayIsInRange(DateTimes.DateRange(organizationToday, organizationToday)))
    }

    @Test
    fun `a range that starts today is current on its first day`() {
        assertTrue(DateTimes.todayIsInRange(DateTimes.DateRange(organizationToday, organizationToday.plusDays(6))))
    }

    @Test
    fun `a range that ends today is current on its last day`() {
        assertTrue(DateTimes.todayIsInRange(DateTimes.DateRange(organizationToday.minusDays(6), organizationToday)))
    }

    @Test
    fun `a range that has not started yet does not contain today`() {
        assertFalse(DateTimes.todayIsInRange(DateTimes.DateRange(organizationToday.plusDays(1), organizationToday.plusDays(7))))
    }

    @Test
    fun `a range that already ended does not contain today`() {
        assertFalse(DateTimes.todayIsInRange(DateTimes.DateRange(organizationToday.minusDays(7), organizationToday.minusDays(1))))
    }
}
