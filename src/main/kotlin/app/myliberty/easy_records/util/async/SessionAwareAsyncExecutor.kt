package app.myliberty.easy_records.util.async

import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.configuration.session.withSession
import org.springframework.stereotype.Component
import java.util.concurrent.Executors

@Component
class SessionAwareAsyncExecutor : AsyncExecutor {

  private val executor = Executors.newCachedThreadPool()

  override fun execute(block: () -> Unit) {
    val session = SessionContextProvider.getSession().copy()
    executor.submit { withSession(session) { block() } }
  }
}
