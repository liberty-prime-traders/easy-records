package app.myliberty.easy_records.cucumber.steps.organizations.core.ledger

import app.myliberty.easy_records.cucumber.support.AccountBalanceReader
import app.myliberty.easy_records.cucumber.support.AccountBalanceRow
import app.myliberty.easy_records.cucumber.support.AsyncPosting
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountTreeSteps(
  private val accountBalanceReader: AccountBalanceReader
) {

  private val accountCodesAssertedInScenario = mutableSetOf<String>()

  @Then("the account balances should be:")
  fun verifyAccountBalances(expectedTable: DataTable) {
    val expectedBalanceByCode = expectedTable.asMaps().associate { row ->
      row.getValue("account") to BigDecimal(row.getValue("balance"))
    }
    val expectedNameByCode = expectedTable.asMaps()
      .filter { row -> row.containsKey("name") }
      .associate { row -> row.getValue("account") to row.getValue("name") }
    accountCodesAssertedInScenario.addAll(expectedBalanceByCode.keys)
    AsyncPosting.untilAsserted("account balances $expectedBalanceByCode") {
      val accountRowByCode = accountBalanceReader.allAccounts().associateBy { it.code }
      expectedNameByCode.forEach { (accountCode, expectedName) ->
        assertEquals(expectedName, accountRowByCode.getValue(accountCode).name, "name of account $accountCode")
      }
      expectedBalanceByCode.forEach { (accountCode, expectedBalance) ->
        val actualBalance = accountRowByCode.getValue(accountCode).currentBalance
        val mismatchMessage = "balance of $accountCode: expected $expectedBalance but was $actualBalance"
        assertEquals(0, expectedBalance.compareTo(actualBalance), mismatchMessage)
      }
    }
  }

  @Then("no other account should have a balance")
  fun verifyNoOtherAccountHasBalance() {
    AsyncPosting.untilAsserted("no account outside $accountCodesAssertedInScenario has a balance") {
      val unexpectedlyNonZero = accountBalanceReader.allAccounts()
        .filter { it.code !in accountCodesAssertedInScenario }
        .filter { it.currentBalance.signum() != 0 }
        .associate { it.code to it.currentBalance }
      val unlistedMessage = "accounts with a balance that the scenario did not list: $unexpectedlyNonZero"
      assertTrue(unexpectedlyNonZero.isEmpty(), unlistedMessage)
    }
  }

  @Then("every parent account should equal the sum of its children")
  fun verifyParentsEqualChildren() {
    AsyncPosting.untilAsserted("parent balances equal the sum of their children") {
      val accountRows = accountBalanceReader.allAccounts()
      val childRowsByParentCode = accountRows
        .filter { it.parentAccountCode != null }
        .groupBy { it.parentAccountCode }
      accountRows.filter { it.code in childRowsByParentCode }.forEach { parentRow ->
        val childRows = childRowsByParentCode.getValue(parentRow.code)
        val expectedBalance = childRows.fold(BigDecimal.ZERO) { total, childRow ->
          total + signedContribution(parentRow, childRow)
        }
        val mismatchMessage =
          "parent ${parentRow.code} balance ${parentRow.currentBalance} differs from its children ($expectedBalance)"
        assertEquals(0, expectedBalance.compareTo(parentRow.currentBalance), mismatchMessage)
      }
    }
  }

  // A contra child (sales discounts under sales revenue) reduces its parent
  private fun signedContribution(parentRow: AccountBalanceRow, childRow: AccountBalanceRow): BigDecimal =
    if (childRow.accountType.isContraOf(parentRow.accountType)) {
      childRow.currentBalance.negate()
    } else {
      childRow.currentBalance
    }
}
