package app.myliberty.easy_records.organizations.business.core.fiscal_period

import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.organizations.business.core.accounting_config.api.OrgAccountingConfigService
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Instant
import java.time.LocalDate
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
}
