package app.myliberty.easy_records.organizations.business.core.ledger.api

import app.myliberty.easy_records.organizations.business.core.account.api.AccountPosting
import app.myliberty.easy_records.organizations.business.core.account.api.AccountService
import app.myliberty.easy_records.organizations.business.core.account.api.EntryType
import app.myliberty.easy_records.organizations.business.core.fiscal_period.api.FiscalPeriodService
import app.myliberty.easy_records.organizations.business.core.ledger.LedgerEntryGroupEntity
import app.myliberty.easy_records.organizations.business.core.ledger.LedgerEntryGroupRepository
import app.myliberty.easy_records.organizations.business.core.ledger.LedgerEntryRepository
import app.myliberty.easy_records.organizations.business.core.ledger.SubledgerBalanceRepository
import app.myliberty.easy_records.organizations.business.core.ledger.SubledgerEntryRepository
import app.myliberty.easy_records.support.WithDefaultOrganizationSession
import app.myliberty.easy_records.util.business.PositiveMoney
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyIterable
import org.mockito.ArgumentMatchers.anySet
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@WithDefaultOrganizationSession
class LedgerPostingServiceDeliveryPathTest {

    private val ledgerEntryGroupRepository = mock(LedgerEntryGroupRepository::class.java)
    private val ledgerEntryRepository = mock(LedgerEntryRepository::class.java)
    private val subledgerEntryRepository = mock(SubledgerEntryRepository::class.java)
    private val subledgerBalanceRepository = mock(SubledgerBalanceRepository::class.java)
    private val accountsAndLedgerLock = mock(AccountsAndLedgerLock::class.java)
    private val fiscalPeriodService = mock(FiscalPeriodService::class.java)
    private val accountService = mock(AccountService::class.java)

    private val ledgerPostingService = LedgerPostingService(
        ledgerEntryGroupRepository,
        ledgerEntryRepository,
        subledgerEntryRepository,
        subledgerBalanceRepository,
        accountsAndLedgerLock,
        fiscalPeriodService,
        accountService
    )

    private val postingDate = LocalDate.of(2026, 1, 1)

    init {
        `when`(fiscalPeriodService.findOpenForDate(postingDate)).thenReturn(UUID.randomUUID())
        `when`(ledgerEntryGroupRepository.save(any(LedgerEntryGroupEntity::class.java))).thenAnswer { invocation ->
            (invocation.arguments[0] as LedgerEntryGroupEntity).also { it.referenceNumber = "LG000001" }
        }
    }

    @Test
    fun `an unbalanced posting is rejected before any lock, check or write`() {
        val unbalancedRequest = postingRequest(
            LedgerEntryRequest.debit("001.001.003", PositiveMoney.of(BigDecimal("20.00"))),
            LedgerEntryRequest.credit("002.001.001", PositiveMoney.of(BigDecimal("19.00")))
        )

        assertThrows(LibertyGenericException::class.java) { ledgerPostingService.post(unbalancedRequest) }

        verifyNoInteractions(accountsAndLedgerLock, accountService, ledgerEntryGroupRepository, ledgerEntryRepository)
    }

    @Test
    fun `a balanced posting locks, checks postability, saves, then patches balances in that order`() {
        val balancedRequest = postingRequest(
            LedgerEntryRequest.debit("001.001.003", PositiveMoney.of(BigDecimal("20.00"))),
            LedgerEntryRequest.credit("002.001.001", PositiveMoney.of(BigDecimal("20.00")))
        )
        val expectedAccountPostings = listOf(
            AccountPosting("001.001.003", BigDecimal("20.00"), EntryType.DEBIT),
            AccountPosting("002.001.001", BigDecimal("20.00"), EntryType.CREDIT)
        )

        ledgerPostingService.post(balancedRequest)

        val postingOrder = inOrder(accountsAndLedgerLock, accountService, ledgerEntryGroupRepository, ledgerEntryRepository)
        postingOrder.verify(accountsAndLedgerLock).acquire(balancedRequest)
        postingOrder.verify(accountService).assertPostable(expectedAccountPostings)
        postingOrder.verify(ledgerEntryGroupRepository).save(any(LedgerEntryGroupEntity::class.java))
        postingOrder.verify(ledgerEntryRepository).saveAll(anyIterable())
        postingOrder.verify(accountService).patchBalances(expectedAccountPostings)
    }

    @Test
    fun `a posting with no legs and no subledger entries saves the group and touches no balances`() {
        ledgerPostingService.post(postingRequest())

        verify(ledgerEntryGroupRepository).save(any(LedgerEntryGroupEntity::class.java))
        verify(subledgerBalanceRepository, never()).findByContactReferenceNumberIn(anySet())
        verify(subledgerEntryRepository, never()).saveAll(anyIterable())
    }

    private fun postingRequest(vararg entries: LedgerEntryRequest) = LedgerPostingRequest(
        sourceReferenceNumber = "DEL-1",
        sourceType = LedgerSourceType.PURCHASE_DELIVERY,
        postingDate = postingDate,
        entries = entries.toList(),
        subledgerEntries = emptyList()
    )
}
