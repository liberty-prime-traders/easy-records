package app.myliberty.easy_records.cucumber.steps.organizations.core.loan

import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.TestDates
import app.myliberty.easy_records.cucumber.support.AccountBalanceReader
import app.myliberty.easy_records.cucumber.support.AuthenticatedRequestFactory
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.ResponseContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactInsertDto
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactType
import app.myliberty.easy_records.organizations.business.core.contact.api.IdentityType
import app.myliberty.easy_records.temp.LoanTestDatabase
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.restassured.response.Response
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoanSteps(
  private val apiClient: ApiClient,
  private val authenticatedRequestFactory: AuthenticatedRequestFactory,
  private val injectContext: InjectContext,
  private val loanTestDatabase: LoanTestDatabase,
  private val accountBalanceReader: AccountBalanceReader
) {

  private val cashAccountCode = "001.001.001"
  private val loansPayableAccountCode = "002.004"
  private val objectMapper = jacksonObjectMapper()

  private var concurrentRoundStatusCodes = emptyList<List<Int>>()

  @Given("a lender named {string} exists")
  fun createLender(companyName: String) = createContact(ContactType.LENDER, companyName)

  @Given("a customer named {string} exists")
  fun createCustomer(companyName: String) = createContact(ContactType.CUSTOMER, companyName)

  @Given("a loan of {string} from the lender exists")
  fun createLoan(amount: String) {
    val loanCreateRequest = mapOf(
      "lenderContactReference" to injectContext.findString(TransientKey.CONTACT_REFERENCE),
      "assetAccountCode" to cashAccountCode,
      "amount" to BigDecimal(amount),
      "loanDate" to today().toString()
    )
    rememberLoan(apiClient.post("/secured/loans", loanCreateRequest))
  }

  @Given("a repayment of {string} against the loan exists")
  fun createRepayment(amount: String) {
    val loanId = injectContext.get(TransientKey.LOAN).toString()
    rememberLoan(apiClient.post("/secured/loans/repayments", repaymentRequest(loanId, amount)))
  }

  @Given("the loan has been voided")
  fun voidLoan() {
    val loanVoidRequest = mapOf("loanId" to injectContext.get(TransientKey.LOAN).toString())
    rememberLoan(apiClient.post("/secured/loans/void", loanVoidRequest))
  }

  @Given("the repayment has been voided")
  fun voidRepayment() {
    val repaymentVoidRequest = mapOf("repaymentId" to injectContext.get(TransientKey.REPAYMENT).toString())
    rememberLoan(apiClient.post("/secured/loans/repayments/void", repaymentVoidRequest))
  }

  @When("I search the loans created in the last day with parameters:")
  fun searchLoansCreatedInTheLastDay(searchParametersJson: String) {
    val searchParameters = objectMapper.readValue<MutableMap<String, Any>>(injectContext.inject(searchParametersJson))
    searchParameters.putAll(lastDayCreatedRange())
    apiClient.post("/secured/loans/search", searchRequest(searchParameters, requestedSize = 50))
  }

  @When("{int} rounds of two simultaneous repayments of {string} are submitted against fresh loans of {string}")
  fun raceTwoRepayments(roundCount: Int, repaymentAmount: String, loanAmount: String) {
    concurrentRoundStatusCodes = (1..roundCount).map {
      createLoan(loanAmount)
      val freshLoanId = injectContext.get(TransientKey.LOAN).toString()
      val repaymentRequest = repaymentRequest(freshLoanId, repaymentAmount)
      submitSimultaneously(
        { postStatusCode("/secured/loans/repayments", repaymentRequest) },
        { postStatusCode("/secured/loans/repayments", repaymentRequest) }
      )
    }
  }

  @When("{int} rounds of a repayment of {string} racing a loan void are submitted against fresh loans of {string}")
  fun raceRepaymentAgainstLoanVoid(roundCount: Int, repaymentAmount: String, loanAmount: String) {
    concurrentRoundStatusCodes = (1..roundCount).map {
      createLoan(loanAmount)
      val freshLoanId = injectContext.get(TransientKey.LOAN).toString()
      val repaymentRequest = repaymentRequest(freshLoanId, repaymentAmount)
      val loanVoidRequest = mapOf("loanId" to freshLoanId, "reason" to "race")
      submitSimultaneously(
        { postStatusCode("/secured/loans/repayments", repaymentRequest) },
        { postStatusCode("/secured/loans/void", loanVoidRequest) }
      )
    }
  }

  @Then("loans payable should equal the sum of outstanding loans")
  fun verifyLoansPayableEqualsOutstandingLoans() {
    val allLoansRequest = searchRequest(lastDayCreatedRange(), requestedSize = 200)
    val searchResponse = apiClient.post("/secured/loans/search", allLoansRequest)
    val outstandingAmounts = searchResponse.jsonPath().getList<Any>("contents.outstanding")
    val outstandingTotal = outstandingAmounts.fold(BigDecimal.ZERO) { runningTotal, outstandingAmount ->
      runningTotal + BigDecimal(outstandingAmount.toString())
    }
    val loansPayableBalance = accountBalanceReader.balanceOf(loansPayableAccountCode)
    assertEquals(
      0,
      outstandingTotal.compareTo(loansPayableBalance),
      "outstanding $outstandingTotal vs loans payable $loansPayableBalance"
    )
  }

  @Then("no loan ledger groups should exist")
  fun verifyNoLoanLedgerGroups() {
    assertEquals(0, loanTestDatabase.countLedgerGroupsOfType("LN"))
  }

  @Then("exactly one request should succeed in every round")
  fun verifyExactlyOneRequestSucceedsInEveryRound() {
    concurrentRoundStatusCodes.forEachIndexed { roundIndex, statusCodes ->
      val roundNumber = roundIndex + 1
      val successCount = statusCodes.count { statusCode -> statusCode == 200 }
      assertEquals(1, successCount, "round $roundNumber statuses: $statusCodes")
      val everyStatusIsExpected = statusCodes.all { statusCode -> statusCode == 200 || statusCode == 400 }
      assertTrue(everyStatusIsExpected, "round $roundNumber statuses: $statusCodes")
    }
  }

  private fun createContact(contactType: ContactType, companyName: String) {
    val contactInsertDto = ContactInsertDto(
      contactTypes = setOf(contactType),
      identityType = IdentityType.ORGANIZATION,
      companyName = companyName
    )
    val response = apiClient.post("/secured/contacts", contactInsertDto)
    check(response.statusCode == 200) { "Failed to create contact '$companyName': ${response.asString()}" }
    injectContext.store(TransientKey.CONTACT, ResponseContext.idFromResponse(response))
    injectContext.storeText(TransientKey.CONTACT_REFERENCE, response.jsonPath().getString("referenceNumber"))
  }

  private fun rememberLoan(response: Response) {
    check(response.statusCode == 200) { "Loan fixture failed: ${response.statusCode} ${response.asString()}" }
    val loanJson = response.jsonPath()
    injectContext.store(TransientKey.LOAN, UUID.fromString(loanJson.getString("id")))
    injectContext.storeText(TransientKey.LOAN_REFERENCE, loanJson.getString("referenceNumber"))
    val activeRepaymentIds = loanJson.getList<String>("repayments.findAll { it.void == null }.id")
    activeRepaymentIds.lastOrNull()?.let { latestActiveRepaymentId ->
      injectContext.store(TransientKey.REPAYMENT, UUID.fromString(latestActiveRepaymentId))
    }
    val activeRepaymentReferences = loanJson.getList<String>("repayments.findAll { it.void == null }.referenceNumber")
    activeRepaymentReferences.lastOrNull()?.let { latestActiveRepaymentReference ->
      injectContext.storeText(TransientKey.REPAYMENT_REFERENCE, latestActiveRepaymentReference)
    }
  }

  private fun repaymentRequest(loanId: String, amount: String) = mapOf(
    "loanId" to loanId,
    "assetAccountCode" to cashAccountCode,
    "amount" to BigDecimal(amount),
    "repaymentDate" to today().toString()
  )

  private fun searchRequest(searchParameters: Map<String, Any>, requestedSize: Int) =
    mapOf("parameters" to searchParameters, "previousCursor" to "", "requestedSize" to requestedSize)

  private fun lastDayCreatedRange() = mapOf(
    "createdFrom" to OffsetDateTime.now(ZoneOffset.UTC).minusDays(1).toString(),
    "createdBefore" to OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).toString()
  )

  private fun postStatusCode(url: String, requestBody: Any): Int =
    authenticatedRequestFactory.jsonRequest().body(requestBody).post(url).statusCode

  private fun submitSimultaneously(firstRequest: () -> Int, secondRequest: () -> Int): List<Int> {
    val startBarrier = CyclicBarrier(2)
    val executor = Executors.newFixedThreadPool(2)
    try {
      val pendingStatusCodes = listOf(firstRequest, secondRequest).map { request ->
        executor.submit<Int> {
          startBarrier.await()
          request()
        }
      }
      return pendingStatusCodes.map { pendingStatusCode -> pendingStatusCode.get(60, TimeUnit.SECONDS) }
    } finally {
      executor.shutdownNow()
    }
  }

  private fun today(): LocalDate = TestDates.today()
}
