package app.myliberty.easy_records.organizations.business.core.fiscal_period

import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.organizations.business.core.account.api.DenormalizedYearEndBalanceTransfer
import app.myliberty.easy_records.organizations.business.core.accounting_config.api.FiscalPeriodCycle
import app.myliberty.easy_records.organizations.business.core.accounting_config.api.OrgAccountingConfigDto
import app.myliberty.easy_records.organizations.business.core.accounting_config.api.OrgAccountingConfigService
import app.myliberty.easy_records.organizations.business.core.fiscal_period.api.YearEndCloseService
import app.myliberty.easy_records.util.business.mappers.UserQualifier
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class YearEndCloseServiceTest {

    private val repository: FiscalPeriodRepository = Mockito.mock(FiscalPeriodRepository::class.java)
    private val mapper: FiscalPeriodMapper = Mockito.mock(FiscalPeriodMapper::class.java)
    private val configService: OrgAccountingConfigService = Mockito.mock(OrgAccountingConfigService::class.java)
    private val denormalizedYearEndBalanceTransfer: DenormalizedYearEndBalanceTransfer =
        Mockito.mock(DenormalizedYearEndBalanceTransfer::class.java)
    private val validator = PeriodCloseValidator(repository, configService)
    private val userQualifier: UserQualifier = Mockito.mock(UserQualifier::class.java)
    private val yearEndCloseService = YearEndCloseService(
        repository, mapper, configService, denormalizedYearEndBalanceTransfer, validator, userQualifier
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
    fun `closing before the accounting configuration exists is refused`() {
        val exception = assertThrows(LibertyGenericException::class.java) { yearEndCloseService.close(UUID.randomUUID()) }

        assertEquals("Accounting configuration has not been initialized", exception.message)
    }

    @Test
    fun `closing a period that does not exist is refused`() {
        givenAccountingConfig()
        val missingId = UUID.randomUUID()

        val exception = assertThrows(LibertyGenericException::class.java) { yearEndCloseService.close(missingId) }

        assertEquals("Fiscal period not found: $missingId", exception.message)
    }

    @Test
    fun `a period that is not a year end is refused before any balance is transferred`() {
        givenAccountingConfig()
        val regularPeriod = givenPeriod(yearEnd = false)

        val exception = assertThrows(LibertyGenericException::class.java) { yearEndCloseService.close(regularPeriod.id!!) }

        assertEquals("Period 'Dec 2000' is not a year-end period", exception.message)
        Mockito.verifyNoInteractions(denormalizedYearEndBalanceTransfer)
    }

    @Test
    fun `a year-end period that has not ended is refused before any balance is transferred`() {
        givenAccountingConfig()
        val futurePeriod = givenPeriod(yearEnd = true, endDate = LocalDate.of(2998, 12, 31))

        val exception = assertThrows(LibertyGenericException::class.java) { yearEndCloseService.close(futurePeriod.id!!) }

        assertEquals("Period 'Dec 2000' has not ended yet and cannot be closed", exception.message)
        assertNull(futurePeriod.closedAt)
        Mockito.verifyNoInteractions(denormalizedYearEndBalanceTransfer)
    }

    @Test
    fun `closing an ended year-end period transfers the balances and stamps who closed it`() {
        givenAccountingConfig()
        val yearEndPeriod = givenPeriod(yearEnd = true)
        Mockito.`when`(repository.countOpenInYear(LocalDate.of(2000, 1, 1), LocalDate.of(2000, 12, 31), yearEndPeriod.id!!))
            .thenReturn(0)
        Mockito.`when`(repository.save(yearEndPeriod)).thenReturn(yearEndPeriod)

        val response = yearEndCloseService.close(yearEndPeriod.id!!)

        Mockito.verify(denormalizedYearEndBalanceTransfer).applyYearEndBalanceTransfer()
        assertNotNull(yearEndPeriod.closedAt)
        assertEquals(sessionUserId, yearEndPeriod.closedBy)
        assertEquals(yearEndPeriod.closedAt, response.closedAt)
        assertEquals("FY2000", response.fiscalYear)
        assertFalse(response.closable)
        assertFalse(response.current)
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

    private fun givenPeriod(yearEnd: Boolean, endDate: LocalDate = LocalDate.of(2000, 12, 31)): FiscalPeriodEntity {
        val period = FiscalPeriodEntity(
            name = "Dec 2000", startDate = LocalDate.of(2000, 12, 1), endDate = endDate, yearEnd = yearEnd
        )
        period.id = UUID.randomUUID()
        Mockito.`when`(repository.findById(period.id!!)).thenReturn(Optional.of(period))
        Mockito.`when`(mapper.toDomainDto(period)).thenAnswer {
            FiscalPeriodDto(
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
        return period
    }
}
