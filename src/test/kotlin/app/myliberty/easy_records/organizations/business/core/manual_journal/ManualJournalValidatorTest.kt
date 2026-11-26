package app.myliberty.easy_records.organizations.business.core.manual_journal

import app.myliberty.easy_records.organizations.business.core.account.api.AccountSelectionCandidate
import app.myliberty.easy_records.organizations.business.core.account.api.EntryType
import app.myliberty.easy_records.organizations.business.core.account.api.RecordingAccountService
import app.myliberty.easy_records.organizations.business.core.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.core.manual_journal.api.ManualJournalLineCreateDto
import app.myliberty.easy_records.util.business.PositiveMoney
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ManualJournalValidatorTest {

    private val candidatesByCode = AccountSelectionCandidate
        .fromAccounts(SystemAccount.entries.map { RecordingAccountService.accountDto(it, accountIsActive = true) })
        .associateBy { it.code }

    private val rentDebitLine = line(SystemAccount.RENT_EXPENSE.code, EntryType.DEBIT, "100.00")
    private val capitalCreditLine = line(SystemAccount.OWNERS_CAPITAL.code, EntryType.CREDIT, "100.00")

    @Test
    fun `a balanced journal on allowlisted accounts has no errors`() {
        val errors = ManualJournalValidator.createErrors(
            "Rent accrual",
            listOf(rentDebitLine, capitalCreditLine),
            candidatesByCode
        )

        assertTrue(errors.isEmpty(), errors.toString())
    }

    @Test
    fun `a blank summary is reported`() {
        val errors = ManualJournalValidator.createErrors(
            "   ",
            listOf(rentDebitLine, capitalCreditLine),
            candidatesByCode
        )

        assertEquals(listOf("Summary is required."), errors)
    }

    @Test
    fun `fewer than two lines is reported`() {
        val errors = ManualJournalValidator.createErrors("Empty", emptyList(), candidatesByCode)

        assertEquals(listOf("At least two lines are required."), errors)
    }

    @Test
    fun `debits that differ from credits are reported`() {
        val unbalancedCreditLine = line(SystemAccount.OWNERS_CAPITAL.code, EntryType.CREDIT, "90.00")

        val errors = ManualJournalValidator.createErrors(
            "Unbalanced",
            listOf(rentDebitLine, unbalancedCreditLine),
            candidatesByCode
        )

        assertEquals(listOf("Debits must equal credits."), errors)
    }

    @Test
    fun `an account outside the manual journal allowlist is reported`() {
        val errors = ManualJournalValidator.createErrors(
            "Cash entry",
            listOf(line(SystemAccount.CASH.code, EntryType.DEBIT, "100.00"), capitalCreditLine),
            candidatesByCode
        )

        assertEquals(listOf("Account 001.001.001 can't be used in a manual journal."), errors)
    }

    @Test
    fun `a parent account is reported as not postable`() {
        val errors = ManualJournalValidator.createErrors(
            "Parent entry",
            listOf(line(SystemAccount.EXPENSES.code, EntryType.DEBIT, "100.00"), capitalCreditLine),
            candidatesByCode
        )

        assertEquals(listOf("Account 005 is not postable."), errors)
    }

    @Test
    fun `an unknown account code is reported as not postable`() {
        val errors = ManualJournalValidator.createErrors(
            "Unknown entry",
            listOf(line("009.999.999", EntryType.DEBIT, "100.00"), capitalCreditLine),
            candidatesByCode
        )

        assertEquals(listOf("Account 009.999.999 is not postable."), errors)
    }

    @Test
    fun `a reversal needs a reason`() {
        assertEquals(
            listOf("Reason is required."),
            ManualJournalValidator.reversalErrors(reason = " ", isReversal = false, alreadyReversed = false)
        )
    }

    @Test
    fun `a reversal cannot be reversed and an original can be reversed only once`() {
        assertEquals(
            listOf("A reversal cannot be reversed."),
            ManualJournalValidator.reversalErrors(reason = "Mistake", isReversal = true, alreadyReversed = false)
        )
        assertEquals(
            listOf("This manual journal has already been reversed."),
            ManualJournalValidator.reversalErrors(reason = "Mistake", isReversal = false, alreadyReversed = true)
        )
        val errors = ManualJournalValidator.reversalErrors(
            reason = "Mistake",
            isReversal = false,
            alreadyReversed = false
        )
        assertTrue(errors.isEmpty(), errors.toString())
    }

    private fun line(accountCode: String, entryType: EntryType, amount: String) = ManualJournalLineCreateDto(
        accountCode = accountCode,
        entryType = entryType,
        amount = PositiveMoney.of(BigDecimal(amount))
    )
}
