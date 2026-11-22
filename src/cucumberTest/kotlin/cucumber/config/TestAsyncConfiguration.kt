package app.myliberty.easy_records.cucumber.config

import app.myliberty.easy_records.util.async.AsyncExecutor
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestAsyncConfiguration {

  // A request that hands work to the executor must not return before the work is done, or the next step races it.
  @Bean
  @Primary
  fun inlineAsyncExecutor(): AsyncExecutor = object : AsyncExecutor {
    override fun execute(block: () -> Unit) = block()
  }
}
