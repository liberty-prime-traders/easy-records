package app.myliberty.easy_records.organizations.business.core.fiscal_period

import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.organizations.business.core.accounting_config.api.FiscalPeriodCycle
import app.myliberty.easy_records.organizations.business.core.accounting_config.api.OrgAccountingConfigDto
import app.myliberty.easy_records.organizations.business.core.accounting_config.api.OrgAccountingConfigService
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class PeriodCloseValidatorTest {

    private val repository: FiscalPeriodRepository = Mockito.mock(FiscalPeriodRepository::class.java)
    private val configService: OrgAccountingConfigService = Mockito.mock(OrgAccountingConfigService::class.java)
    private val periodCloseValidator = PeriodCloseValidator(repository, configService)

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

    // The closed rule runs before the ended-date rule, so a closed period with a future end date reports as closed.
    @Test
    fun `an already closed period is refused as closed before the ended-date rule`() {
        val closedPeriod = FiscalPeriodEntity(
            name = "Future Period",
            startDate = LocalDate.of(2998, 1, 1),
            endDate = LocalDate.of(2998, 12, 31),
            closedAt = Instant.parse("2998-01-02T00:00:00Z"),
            closedBy = UUID.randomUUID()
        )

        val exception = assertThrows(LibertyGenericException::class.java) {
            periodCloseValidator.assertCanClose(closedPeriod)
        }

        assertEquals("Period 'Future Period' is already closed", exception.message)
    }

    @Test
    fun `a period whose end date is still ahead is refused as not yet ended`() {
        val openPeriod = FiscalPeriodEntity(
            name = "Future Period",
            startDate = LocalDate.of(2998, 1, 1),
            endDate = LocalDate.of(2998, 12, 31)
        )

        val exception = assertThrows(LibertyGenericException::class.java) {
            periodCloseValidator.assertCanClose(openPeriod)
        }

        assertEquals("Period 'Future Period' has not ended yet and cannot be closed", exception.message)
    }

    @Test
    fun `an ended period that is not a year end is accepted without looking at its siblings`() {
        val endedPeriod = endedPeriodEntity(yearEnd = false)

        assertDoesNotThrow { periodCloseValidator.assertCanClose(endedPeriod) }

        Mockito.verifyNoInteractions(repository)
    }

    @Test
    fun `a year-end period with an open period left in its fiscal year is refused`() {
        val yearEndPeriod = endedPeriodEntity(yearEnd = true)
        givenAccountingConfig()
        Mockito.`when`(repository.countOpenInYear(fiscalYearStart, fiscalYearEnd, yearEndPeriod.id!!)).thenReturn(1)

        val exception = assertThrows(LibertyGenericException::class.java) {
            periodCloseValidator.assertCanClose(yearEndPeriod)
        }

        assertEquals("All periods in the fiscal year must be closed before closing 'Ended Period'", exception.message)
    }

    @Test
    fun `a year-end period whose fiscal year has no other open period is accepted`() {
        val yearEndPeriod = endedPeriodEntity(yearEnd = true)
        givenAccountingConfig()
        Mockito.`when`(repository.countOpenInYear(fiscalYearStart, fiscalYearEnd, yearEndPeriod.id!!)).thenReturn(0)

        assertDoesNotThrow { periodCloseValidator.assertCanClose(yearEndPeriod) }
    }

    @Test
    fun `a year-end period cannot be checked before the accounting configuration exists`() {
        val yearEndPeriod = endedPeriodEntity(yearEnd = true)
        Mockito.`when`(configService.getConfig()).thenReturn(null)

        val exception = assertThrows(LibertyGenericException::class.java) {
            periodCloseValidator.assertCanClose(yearEndPeriod)
        }

        assertEquals("Accounting configuration has not been initialized", exception.message)
    }

    @Test
    fun `a closed period is not closable`() {
        val closedDto = periodDto(endDate = endedDate, yearEnd = false, closedAt = Instant.parse("2001-01-02T00:00:00Z"))

        assertFalse(periodCloseValidator.isClosable(closedDto))
        assertFalse(periodCloseValidator.isClosable(closedDto, emptyList()))
    }

    @Test
    fun `a period that has not ended is not closable`() {
        val futureDto = periodDto(endDate = LocalDate.of(2998, 12, 31), yearEnd = false)

        assertFalse(periodCloseValidator.isClosable(futureDto))
        assertFalse(periodCloseValidator.isClosable(futureDto, emptyList()))
    }

    @Test
    fun `an ended period that is not a year end is closable`() {
        val endedDto = periodDto(endDate = endedDate, yearEnd = false)

        assertTrue(periodCloseValidator.isClosable(endedDto))
        assertTrue(periodCloseValidator.isClosable(endedDto, listOf(periodDto(endDate = endedDate, yearEnd = false))))
    }

    @Test
    fun `an ended year-end period is closable only when no other period of its fiscal year is open`() {
        val yearEndDto = periodDto(endDate = fiscalYearEnd, yearEnd = true)
        givenAccountingConfig()
        Mockito.`when`(repository.countOpenInYear(fiscalYearStart, fiscalYearEnd, yearEndDto.id)).thenReturn(1, 0)

        assertFalse(periodCloseValidator.isClosable(yearEndDto))
        assertTrue(periodCloseValidator.isClosable(yearEndDto))
    }

    @Test
    fun `a year-end period is closable against the given peers only when every peer other than itself is closed`() {
        val yearEndDto = periodDto(endDate = fiscalYearEnd, yearEnd = true)
        val closedPeer = periodDto(endDate = endedDate, yearEnd = false, closedAt = Instant.parse("2001-01-02T00:00:00Z"))
        val openPeer = periodDto(endDate = endedDate, yearEnd = false)

        assertTrue(periodCloseValidator.isClosable(yearEndDto, listOf(yearEndDto, closedPeer)))
        assertFalse(periodCloseValidator.isClosable(yearEndDto, listOf(yearEndDto, closedPeer, openPeer)))
    }

    private val endedDate = LocalDate.of(2000, 6, 30)
    private val fiscalYearStart = LocalDate.of(2000, 1, 1)
    private val fiscalYearEnd = LocalDate.of(2000, 12, 31)

    private fun givenAccountingConfig() {
        Mockito.`when`(configService.getConfig()).thenReturn(
            OrgAccountingConfigDto(
                id = UUID.randomUUID(),
                createdById = UUID.randomUUID(),
                createdOn = OffsetDateTime.parse("2000-01-01T00:00:00Z"),
                fiscalYearEndMonth = 12,
                fiscalPeriodCycle = FiscalPeriodCycle.MONTHLY,
                periodWeekStartDay = DayOfWeek.MONDAY,
                periodPrepDays = 30
            )
        )
    }

    private fun endedPeriodEntity(yearEnd: Boolean): FiscalPeriodEntity {
        val endDate = if (yearEnd) fiscalYearEnd else endedDate
        return FiscalPeriodEntity(name = "Ended Period", startDate = fiscalYearStart, endDate = endDate, yearEnd = yearEnd)
            .also { it.id = UUID.randomUUID() }
    }

    private fun periodDto(endDate: LocalDate, yearEnd: Boolean, closedAt: Instant? = null) = FiscalPeriodDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.parse("2000-01-01T00:00:00Z"),
        name = "Ended Period",
        startDate = fiscalYearStart,
        endDate = endDate,
        yearEnd = yearEnd,
        stub = false,
        closedAt = closedAt,
        closedBy = null
    )
}
