package app.myliberty.easy_records.organizations.business.core.fiscal_period

import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.organizations.business.core.accounting_config.api.FiscalPeriodCycle
import app.myliberty.easy_records.organizations.business.core.accounting_config.api.OrgAccountingConfigDto
import app.myliberty.easy_records.organizations.business.core.accounting_config.api.OrgAccountingConfigService
import app.myliberty.easy_records.organizations.business.core.fiscal_period.api.FiscalPeriodRenameDto
import app.myliberty.easy_records.organizations.business.core.fiscal_period.api.FiscalPeriodService
import app.myliberty.easy_records.util.business.mappers.UserQualifier
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class FiscalPeriodServiceTest {

    private val repository: FiscalPeriodRepository = Mockito.mock(FiscalPeriodRepository::class.java)
    private val mapper: FiscalPeriodMapper = Mockito.mock(FiscalPeriodMapper::class.java)
    private val configService: OrgAccountingConfigService = Mockito.mock(OrgAccountingConfigService::class.java)
    private val generationService: FiscalPeriodGenerationService = Mockito.mock(FiscalPeriodGenerationService::class.java)
    private val validator = PeriodCloseValidator(repository, configService)
    private val userQualifier: UserQualifier = Mockito.mock(UserQualifier::class.java)
    private val fiscalPeriodService = FiscalPeriodService(
        repository, mapper, configService, generationService, validator, userQualifier
    )

    private val sessionUserId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SessionContextProvider.setSession(
            SessionContext(
                systemUserId = sessionUserId,
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC")
            )
        )
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `nudging asks the generation service to generate periods`() {
        fiscalPeriodService.nudgePeriodGeneration()

        Mockito.verify(generationService).generatePeriods()
    }

    @Test
    fun `the open period for a date is the one the repository finds`() {
        val openPeriodId = UUID.randomUUID()
        val date = LocalDate.of(2030, 3, 4)
        Mockito.`when`(repository.findOpenContaining(date)).thenReturn(openPeriodId)

        assertEquals(openPeriodId, fiscalPeriodService.findOpenForDate(date))
        assertEquals(openPeriodId, fiscalPeriodService.requireOpenForDate(date))
    }

    @Test
    fun `requiring an open period for a date nobody covers is refused with the date`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            fiscalPeriodService.requireOpenForDate(LocalDate.of(2030, 3, 4))
        }

        assertEquals("No open fiscal period for date 2030-03-04", exception.message)
    }

    @Test
    fun `listing periods before the accounting configuration exists is refused`() {
        val exception = assertThrows(LibertyGenericException::class.java) { fiscalPeriodService.getAll() }

        assertEquals("Accounting configuration has not been initialized", exception.message)
    }

    @Test
    fun `a year-end period is closable only once every other period of its fiscal year is closed`() {
        givenAccountingConfig()
        val openPeer = givenPeriod("Mar 2000", LocalDate.of(2000, 3, 1), LocalDate.of(2000, 3, 31))
        val yearEndPeriod = givenPeriod("Dec 2000", LocalDate.of(2000, 12, 1), LocalDate.of(2000, 12, 31), yearEnd = true)
        val otherYearPeriod = givenPeriod("Mar 2001", LocalDate.of(2001, 3, 1), LocalDate.of(2001, 3, 31))
        Mockito.`when`(repository.findAll()).thenReturn(listOf(openPeer, yearEndPeriod, otherYearPeriod))

        val responsesByName = fiscalPeriodService.getAll().associateBy { it.name }

        assertTrue(responsesByName.getValue("Mar 2000").closable)
        assertFalse(responsesByName.getValue("Dec 2000").closable)
        assertEquals("FY2000", responsesByName.getValue("Dec 2000").fiscalYear)
        assertTrue(responsesByName.getValue("Mar 2001").closable)
        assertFalse(responsesByName.getValue("Mar 2000").current)
    }

    @Test
    fun `a listed period reports whether it covers today, is a stub and who closed it`() {
        givenAccountingConfig()
        val closerId = UUID.randomUUID()
        val coveringPeriod = givenPeriod("Covering", LocalDate.of(2000, 1, 1), LocalDate.of(2998, 12, 31))
        val closedStubPeriod = givenPeriod("Stub Mar 2000", LocalDate.of(2000, 3, 1), LocalDate.of(2000, 3, 31), stub = true)
        closedStubPeriod.closedAt = java.time.Instant.parse("2000-04-01T00:00:00Z")
        closedStubPeriod.closedBy = closerId
        Mockito.`when`(userQualifier.getUserFullName(closerId)).thenReturn("Casey Closer")
        Mockito.`when`(repository.findAll()).thenReturn(listOf(coveringPeriod, closedStubPeriod))

        val responsesByName = fiscalPeriodService.getAll().associateBy { it.name }

        assertTrue(responsesByName.getValue("Covering").current)
        assertFalse(responsesByName.getValue("Covering").stub)
        assertNull(responsesByName.getValue("Covering").closedBy)
        assertFalse(responsesByName.getValue("Stub Mar 2000").current)
        assertTrue(responsesByName.getValue("Stub Mar 2000").stub)
        assertEquals("Casey Closer", responsesByName.getValue("Stub Mar 2000").closedBy)
    }

    @Test
    fun `closing periods that do not all exist is refused naming the missing ones`() {
        val existingPeriod = givenPeriod("Mar 2000", LocalDate.of(2000, 3, 1), LocalDate.of(2000, 3, 31))
        val missingId = UUID.randomUUID()
        val requestedIds = setOf(existingPeriod.id!!, missingId)
        Mockito.`when`(repository.findByIdIn(requestedIds)).thenReturn(listOf(existingPeriod))

        val exception = assertThrows(LibertyGenericException::class.java) { fiscalPeriodService.close(requestedIds) }

        assertEquals("Fiscal periods not found: [$missingId]", exception.message)
        assertNull(existingPeriod.closedAt)
    }

    @Test
    fun `closing a year-end period through the regular close is refused`() {
        val yearEndPeriod = givenPeriod("Dec 2000", LocalDate.of(2000, 12, 1), LocalDate.of(2000, 12, 31), yearEnd = true)
        val requestedIds = setOf(yearEndPeriod.id!!)
        Mockito.`when`(repository.findByIdIn(requestedIds)).thenReturn(listOf(yearEndPeriod))

        val exception = assertThrows(LibertyGenericException::class.java) { fiscalPeriodService.close(requestedIds) }

        assertEquals("Period 'Dec 2000' is a year-end period — use the year-end close endpoint", exception.message)
        assertNull(yearEndPeriod.closedAt)
    }

    @Test
    fun `closing a period that has not ended is refused and leaves it open`() {
        val futurePeriod = givenPeriod("Future", LocalDate.of(2998, 1, 1), LocalDate.of(2998, 1, 31))
        val requestedIds = setOf(futurePeriod.id!!)
        Mockito.`when`(repository.findByIdIn(requestedIds)).thenReturn(listOf(futurePeriod))

        val exception = assertThrows(LibertyGenericException::class.java) { fiscalPeriodService.close(requestedIds) }

        assertEquals("Period 'Future' has not ended yet and cannot be closed", exception.message)
        assertNull(futurePeriod.closedAt)
        Mockito.verify(repository, Mockito.never()).saveAll(Mockito.anyList())
    }

    @Test
    fun `closing an ended period stamps who closed it and when`() {
        givenAccountingConfig()
        val endedPeriod = givenPeriod("Mar 2000", LocalDate.of(2000, 3, 1), LocalDate.of(2000, 3, 31))
        val requestedIds = setOf(endedPeriod.id!!)
        val foundPeriods = listOf(endedPeriod)
        Mockito.`when`(repository.findByIdIn(requestedIds)).thenReturn(foundPeriods)
        Mockito.`when`(repository.saveAll(foundPeriods)).thenReturn(foundPeriods)

        val responses = fiscalPeriodService.close(requestedIds)

        assertNotNull(endedPeriod.closedAt)
        assertEquals(sessionUserId, endedPeriod.closedBy)
        assertEquals(1, responses.size)
        assertEquals(endedPeriod.closedAt, responses.single().closedAt)
        assertFalse(responses.single().closable)
    }

    @Test
    fun `renaming a period that does not exist is refused`() {
        val missingId = UUID.randomUUID()

        val exception = assertThrows(LibertyGenericException::class.java) {
            fiscalPeriodService.rename(FiscalPeriodRenameDto(missingId, "Anything"))
        }

        assertEquals("Fiscal period not found: $missingId", exception.message)
    }

    @Test
    fun `renaming a period to its own name apart from case saves nothing`() {
        givenAccountingConfig()
        val period = givenPeriod("Mar 2000", LocalDate.of(2000, 3, 1), LocalDate.of(2000, 3, 31))
        Mockito.`when`(repository.findById(period.id!!)).thenReturn(java.util.Optional.of(period))

        val response = fiscalPeriodService.rename(FiscalPeriodRenameDto(period.id!!, "MAR 2000"))

        assertEquals("Mar 2000", response.name)
        Mockito.verify(repository, Mockito.never()).save(period)
    }

    @Test
    fun `renaming a period to a name another period of the same fiscal year holds is refused`() {
        givenAccountingConfig()
        val period = givenPeriod("Mar 2000", LocalDate.of(2000, 3, 1), LocalDate.of(2000, 3, 31))
        val sibling = givenPeriod("April Close", LocalDate.of(2000, 4, 1), LocalDate.of(2000, 4, 30))
        Mockito.`when`(repository.findById(period.id!!)).thenReturn(java.util.Optional.of(period))
        Mockito.`when`(repository.findPeriodsInGivenYear(LocalDate.of(2000, 1, 1), LocalDate.of(2000, 12, 31)))
            .thenReturn(listOf(period, sibling))

        val exception = assertThrows(LibertyGenericException::class.java) {
            fiscalPeriodService.rename(FiscalPeriodRenameDto(period.id!!, "april close"))
        }

        assertEquals("A period named 'april close' already exists in this fiscal year", exception.message)
        assertEquals("Mar 2000", period.name)
    }

    @Test
    fun `renaming a period to a free name saves the new name`() {
        givenAccountingConfig()
        val period = givenPeriod("Mar 2000", LocalDate.of(2000, 3, 1), LocalDate.of(2000, 3, 31))
        Mockito.`when`(repository.findById(period.id!!)).thenReturn(java.util.Optional.of(period))
        Mockito.`when`(repository.findPeriodsInGivenYear(LocalDate.of(2000, 1, 1), LocalDate.of(2000, 12, 31)))
            .thenReturn(listOf(period))
        Mockito.`when`(repository.save(period)).thenReturn(period)

        val response = fiscalPeriodService.rename(FiscalPeriodRenameDto(period.id!!, "Spring Close"))

        assertEquals("Spring Close", response.name)
        assertEquals("Spring Close", period.name)
        assertTrue(response.closable)
    }

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

    private fun givenPeriod(
        name: String,
        startDate: LocalDate,
        endDate: LocalDate,
        yearEnd: Boolean = false,
        stub: Boolean = false
    ): FiscalPeriodEntity {
        val period = FiscalPeriodEntity(name = name, startDate = startDate, endDate = endDate, yearEnd = yearEnd, stub = stub)
        period.id = UUID.randomUUID()
        Mockito.`when`(mapper.toDomainDto(period)).thenAnswer { toDto(period) }
        return period
    }

    private fun toDto(period: FiscalPeriodEntity) = FiscalPeriodDto(
        id = period.id!!,
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.parse("2000-01-01T00:00:00Z"),
        name = period.name,
        startDate = period.startDate,
        endDate = period.endDate,
        yearEnd = period.yearEnd,
        stub = period.stub,
        closedAt = period.closedAt,
        closedBy = period.closedBy
    )
}
