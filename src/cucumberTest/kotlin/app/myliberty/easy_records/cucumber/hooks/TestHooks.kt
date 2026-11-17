package app.myliberty.easy_records.cucumber.hooks

import io.cucumber.java.After
import io.cucumber.java.Before
import app.myliberty.easy_records.cucumber.support.KafkaConsumerTestSupport
import app.myliberty.easy_records.cucumber.support.cleanup.CacheCleaner
import app.myliberty.easy_records.cucumber.support.cleanup.TestDatabaseCleaner
import app.myliberty.easy_records.cucumber.support.context.AuthContext
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.KafkaContext
import app.myliberty.easy_records.cucumber.support.context.ResponseContext

class TestHooks(
  private val responseContext: ResponseContext,
  private val testDatabaseCleaner: TestDatabaseCleaner,
  private val kafkaConsumerTestSupport: KafkaConsumerTestSupport,
  private val injectContext: InjectContext,
  private val authContext: AuthContext,
  private val kafkaContext: KafkaContext,
  private val cacheCleaner: CacheCleaner,
) {

  @Before
  fun beforeScenario() {
    testDatabaseCleaner.clean()
    injectContext.clear()
    kafkaContext.reset()
    cacheCleaner.clearAllCaches()
    authContext.initialize()
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
}
