package app.myliberty.easy_records.cucumber.hooks

import io.cucumber.java.After
import io.cucumber.java.Before
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.KafkaConsumerTestSupport
import app.myliberty.easy_records.cucumber.support.cleanup.CacheCleaner
import app.myliberty.easy_records.cucumber.support.cleanup.TestDatabaseCleaner
import app.myliberty.easy_records.cucumber.support.context.AuthContext
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.KafkaContext
import app.myliberty.easy_records.cucumber.support.context.ResponseContext
import app.myliberty.easy_records.platform.business.feature.api.Feature

class TestHooks(
  private val responseContext: ResponseContext,
  private val testDatabaseCleaner: TestDatabaseCleaner,
  private val kafkaConsumerTestSupport: KafkaConsumerTestSupport,
  private val injectContext: InjectContext,
  private val authContext: AuthContext,
  private val kafkaContext: KafkaContext,
  private val cacheCleaner: CacheCleaner,
  private val apiClient: ApiClient,
) {

  @Before
  fun beforeScenario() {
    testDatabaseCleaner.clean()
    injectContext.clear()
    kafkaContext.reset()
    cacheCleaner.clearAllCaches()
    authContext.initialize()
    activateDefaultFeatures()
    generateOpenFiscalPeriod()
    responseContext.reset()
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

  // Generation starts from yesterday on an empty calendar, so the period covering today is open.
  private fun generateOpenFiscalPeriod() {
    val response = apiClient.post("/secured/fiscal-periods/nudge")
    check(response.statusCode == 200) { "Failed to generate fiscal periods: ${response.asString()}" }
  }
}
