package app.myliberty.easy_records.cucumber.support

import app.myliberty.easy_records.support.TestConstants
import org.awaitility.Awaitility.await
import org.awaitility.core.ConditionTimeoutException
import java.time.Duration

object AsyncPosting {

  private const val QUIET_WINDOW_MS = 1_500L

  fun untilAsserted(description: String, assertion: () -> Unit) {
    try {
      await().alias(description)
        .atMost(Duration.ofMillis(TestConstants.Timeouts.KAFKA_SYNC_MS))
        .pollDelay(Duration.ZERO)
        .pollInterval(Duration.ofMillis(TestConstants.Timeouts.KAFKA_POLL_INTERVAL_MS))
        .untilAsserted { assertion() }
    } catch (timeout: ConditionTimeoutException) {
      throw AssertionError("$description. ${timeout.message}. $TIMEOUT_SUSPECTS", timeout)
    }
  }

  // An absence cannot be polled into existence, so it must hold for a quiet window to rule out a late consumer write.
  fun staysTrue(description: String, condition: () -> Boolean) {
    try {
      await().alias(description)
        .during(Duration.ofMillis(QUIET_WINDOW_MS))
        .atMost(Duration.ofMillis(TestConstants.Timeouts.KAFKA_SYNC_MS))
        .pollInterval(Duration.ofMillis(TestConstants.Timeouts.KAFKA_POLL_INTERVAL_MS))
        .until { condition() }
    } catch (timeout: ConditionTimeoutException) {
      throw AssertionError("$description. ${timeout.message}", timeout)
    }
  }

  private const val TIMEOUT_SUSPECTS =
    "Suspects in order: the event left before the commit; a cache outside the cache manager; the consumer threw " +
      "(read its log first)"
}
