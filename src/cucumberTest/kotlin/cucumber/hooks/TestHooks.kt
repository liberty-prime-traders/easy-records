package app.myliberty.easy_records.cucumber.hooks

import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.Scenario
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.TestDates
import app.myliberty.easy_records.cucumber.support.initialization.BoilerPlateDataInitializer
import app.myliberty.easy_records.cucumber.support.initialization.PastFiscalPeriodSeeder
import app.myliberty.easy_records.cucumber.support.KafkaConsumerTestSupport
import app.myliberty.easy_records.cucumber.support.cleanup.CacheCleaner
import app.myliberty.easy_records.cucumber.support.cleanup.TestDatabaseCleaner
import app.myliberty.easy_records.cucumber.support.context.AuthContext
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.KafkaContext
import app.myliberty.easy_records.cucumber.support.context.ResponseContext
import app.myliberty.easy_records.cucumber.support.context.PersistentKey
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.platform.business.organization.OrganizationCache
import java.net.URI

class TestHooks(
  private val responseContext: ResponseContext,
  private val testDatabaseCleaner: TestDatabaseCleaner,
  private val kafkaConsumerTestSupport: KafkaConsumerTestSupport,
  private val injectContext: InjectContext,
  private val authContext: AuthContext,
  private val kafkaContext: KafkaContext,
  private val cacheCleaner: CacheCleaner,
  private val apiClient: ApiClient,
  private val boilerPlateDataInitializer: BoilerPlateDataInitializer,
  private val organizationCache: OrganizationCache,
  private val pastFiscalPeriodSeeder: PastFiscalPeriodSeeder,
) {

  @Before
  fun beforeScenario(scenario: Scenario) {
    val continuesSharedState = SHARED_STATE_TAG in scenario.sourceTagNames && scenario.uri == featureWithFreshState
    if (!continuesSharedState) {
      resetDatabaseState(regularFiscalPeriod = REGULAR_FISCAL_PERIOD_TAG in scenario.sourceTagNames)
      featureWithFreshState = scenario.uri
    }
    injectContext.clear()
    kafkaContext.reset()
    authContext.initialize()
    responseContext.reset()
    initializeOrganizationSession()
  }

  @Before(value = "@past-fiscal-periods", order = AFTER_FRESH_STATE)
  fun seedPastFiscalPeriod() {
    pastFiscalPeriodSeeder.seed()
  }

  private fun initializeOrganizationSession() {
    val organizationId = injectContext.get(PersistentKey.ORGANIZATION)
    val organization = organizationCache.getAllOrganizations().first { it.id == organizationId }
    SessionContextProvider.initOrganization(organization)
  }

  @After
  fun clearSession() {
    SessionContextProvider.clear()
  }

  private fun resetDatabaseState(regularFiscalPeriod: Boolean) {
    testDatabaseCleaner.clean()
    cacheCleaner.clearAllCaches()
    authContext.authenticateAsPlatformAdmin()
    activateDefaultFeatures()
    if (regularFiscalPeriod) moveFiscalYearEndAwayFromToday()
    generateOpenFiscalPeriod()
    seedOrganizationDefaults()
    boilerPlateDataInitializer.ensureDefaultMemberships()
  }

  @Before("@publishes-to-kafka")
  fun beforePublishKafkaScenario() {
    kafkaConsumerTestSupport.subscribeCatalogEventsFromLatest()
  }

  @After("@publishes-to-kafka")
  fun closePublishKafkaConsumer() {
    kafkaConsumerTestSupport.closeCatalogEventsConsumer()
  }

  @Before("@consumes-from-kafka")
  fun beforeKafkaConsumerScenario() {
    kafkaConsumerTestSupport.prepareConsumerScenario()
  }

  @After("@consumes-from-kafka")
  fun stopKafkaListenersForConsumerScenarios() {
    kafkaConsumerTestSupport.stopKafkaListeners()
  }

  // The cleaner truncates organization_feature and the seeded chart of accounts, so they are re-created per scenario.
  private fun activateDefaultFeatures() {
    val response = apiClient.post("/secured/org-features/activate", setOf(Feature.CHART_OF_ACCOUNTS))
    check(response.statusCode == 200) { "Failed to activate default features: ${response.asString()}" }
  }

  // Runs every OrgDataSeeder. The test AsyncExecutor is inline, so the data exists when the call returns.
  private fun seedOrganizationDefaults() {
    val response = apiClient.post("/secured/admin-data/seed-defaults")
    check(response.statusCode == 200) { "Failed to seed organization defaults: ${response.asString()}" }
  }

  // The default configuration ends the fiscal year in the month it is created, so the first period is always a
  // year-end one. Six months out makes the period covering today an ordinary one.
  private fun moveFiscalYearEndAwayFromToday() {
    val fiscalYearEndMonth = (TestDates.today().monthValue + 5) % MONTHS_IN_YEAR + 1
    val response = apiClient.put("/secured/accounting-config", mapOf("fiscalYearEndMonth" to fiscalYearEndMonth))
    check(response.statusCode == 200) { "Failed to move the fiscal year end: ${response.asString()}" }
  }

  // Generation starts today on an empty calendar, so no period is in the past; @past-fiscal-periods adds one. Near a
  // period end it also creates the next period, so the latest period is not always today's.
  private fun generateOpenFiscalPeriod() {
    val response = apiClient.post("/secured/fiscal-periods/nudge")
    check(response.statusCode == 200) { "Failed to generate fiscal periods: ${response.asString()}" }
  }

  companion object {
    private const val SHARED_STATE_TAG = "@shared-state"
    private const val AFTER_FRESH_STATE = 20000
    private const val REGULAR_FISCAL_PERIOD_TAG = "@regular-fiscal-period"
    private const val MONTHS_IN_YEAR = 12

    // Hooks are built per scenario, so the feature that last got clean data is kept here.
    private var featureWithFreshState: URI? = null
  }
}
