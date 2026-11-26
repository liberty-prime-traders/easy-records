package app.myliberty.easy_records.cucumber.steps.organizations.core.ledger

import app.myliberty.easy_records.cucumber.support.AsyncPosting
import app.myliberty.easy_records.cucumber.support.LedgerEntryRow
import app.myliberty.easy_records.cucumber.support.LedgerRowReader
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.organizations.business.core.account.api.EntryType
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LedgerRowSteps(
  private val ledgerRowReader: LedgerRowReader,
  private val injectContext: InjectContext
) {

  @Then("the ledger group for {string} should have source type {string}")
  fun verifyLedgerGroupSourceType(sourceReference: String, expectedSourceType: String) {
    val resolvedReference = injectContext.inject(sourceReference)
    AsyncPosting.untilAsserted("ledger group for $resolvedReference with source type $expectedSourceType") {
      val groupRows = ledgerRowReader.groupsFor(resolvedReference)
      assertEquals(1, groupRows.size, "ledger groups for $resolvedReference: $groupRows")
      assertEquals(expectedSourceType, groupRows.single().sourceType.name)
    }
  }

  @Then("the ledger entries for {string} should be exactly:")
  fun verifyLedgerEntries(sourceReference: String, expectedTable: DataTable) {
    val resolvedReference = injectContext.inject(sourceReference)
    val expectedLines = expectedTable.asMaps().map { row ->
      entryLine(
        injectContext.inject(row.getValue("account")),
        EntryType.valueOf(row.getValue("type").uppercase()),
        BigDecimal(row.getValue("amount"))
      )
    }.sorted()
    AsyncPosting.untilAsserted("ledger entries for $resolvedReference") {
      val actualLines = ledgerRowReader.entriesFor(resolvedReference).map(::entryLine).sorted()
      assertEquals(expectedLines, actualLines, "ledger entries for $resolvedReference (account|type|amount)")
    }
  }

  @Then("the ledger entries for {string} should balance")
  fun verifyLedgerEntriesBalance(sourceReference: String) {
    val resolvedReference = injectContext.inject(sourceReference)
    AsyncPosting.untilAsserted("ledger entries for $resolvedReference balance") {
      val entryRows = ledgerRowReader.entriesFor(resolvedReference)
      assertTrue(entryRows.isNotEmpty(), "no ledger entries for $resolvedReference")
      val debitTotal = totalOf(entryRows, EntryType.DEBIT)
      val creditTotal = totalOf(entryRows, EntryType.CREDIT)
      assertEquals(0, debitTotal.compareTo(creditTotal), "debits $debitTotal differ from credits $creditTotal")
    }
  }

  @Then("no ledger group should exist for {string}")
  fun verifyNoLedgerGroup(sourceReference: String) {
    val resolvedReference = injectContext.inject(sourceReference)
    AsyncPosting.staysTrue("no ledger group for $resolvedReference") {
      ledgerRowReader.groupsFor(resolvedReference).isEmpty()
    }
  }

  private fun totalOf(entryRows: List<LedgerEntryRow>, entryType: EntryType): BigDecimal =
    entryRows.filter { it.entryType == entryType }.fold(BigDecimal.ZERO) { total, row -> total + row.amount }

  private fun entryLine(entryRow: LedgerEntryRow): String =
    entryLine(entryRow.accountCode, entryRow.entryType, entryRow.amount)

  private fun entryLine(accountCode: String, entryType: EntryType, amount: BigDecimal): String =
    "$accountCode|${entryType.name}|${amount.stripTrailingZeros().toPlainString()}"
}
