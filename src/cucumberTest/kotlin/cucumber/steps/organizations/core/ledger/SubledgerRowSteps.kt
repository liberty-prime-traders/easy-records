package app.myliberty.easy_records.cucumber.steps.organizations.core.ledger

import app.myliberty.easy_records.cucumber.support.AsyncPosting
import app.myliberty.easy_records.cucumber.support.LedgerRowReader
import app.myliberty.easy_records.cucumber.support.SubledgerEntryRow
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import java.math.BigDecimal
import kotlin.test.assertEquals

class SubledgerRowSteps(
  private val ledgerRowReader: LedgerRowReader,
  private val injectContext: InjectContext
) {

  @Then("the subledger entries for {string} should be exactly:")
  fun verifySubledgerEntries(sourceReference: String, expectedTable: DataTable) {
    val resolvedReference = injectContext.inject(sourceReference)
    val expectedLines = expectedTable.asMaps().map { row ->
      entryLine(
        injectContext.inject(row.getValue("contact")),
        BigDecimal(row.getValue("payable")),
        BigDecimal(row.getValue("receivable")),
        BigDecimal(row.getValue("running payable")),
        BigDecimal(row.getValue("running receivable"))
      )
    }.sorted()
    AsyncPosting.untilAsserted("subledger entries for $resolvedReference") {
      val actualLines = ledgerRowReader.subledgerEntriesFor(resolvedReference).map(::entryLine).sorted()
      assertEquals(
        expectedLines,
        actualLines,
        "subledger entries for $resolvedReference (contact|payable|receivable|running payable|running receivable)"
      )
    }
  }

  @Then("no subledger entries should exist for {string}")
  fun verifyNoSubledgerEntries(sourceReference: String) {
    val resolvedReference = injectContext.inject(sourceReference)
    AsyncPosting.staysTrue("no subledger entries for $resolvedReference") {
      ledgerRowReader.subledgerEntriesFor(resolvedReference).isEmpty()
    }
  }

  private fun entryLine(entryRow: SubledgerEntryRow): String = entryLine(
    entryRow.contactReferenceNumber,
    entryRow.payableAmount,
    entryRow.receivableAmount,
    entryRow.runningPayable,
    entryRow.runningReceivable
  )

  private fun entryLine(
    contactReference: String,
    payable: BigDecimal,
    receivable: BigDecimal,
    runningPayable: BigDecimal,
    runningReceivable: BigDecimal
  ): String =
    "$contactReference|${plain(payable)}|${plain(receivable)}|${plain(runningPayable)}|${plain(runningReceivable)}"

  private fun plain(amount: BigDecimal): String = amount.stripTrailingZeros().toPlainString()
}
