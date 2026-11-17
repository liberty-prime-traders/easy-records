package app.myliberty.easy_records.support

import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import java.util.UUID

// Runs before the test class's own @BeforeEach, so a test that sets its own session still wins
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(DefaultOrganizationSessionExtension::class)
annotation class WithDefaultOrganizationSession

class DefaultOrganizationSessionExtension : BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(extensionContext: ExtensionContext) {
        SessionContextProvider.setSession(
            SessionContext(organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"))
        )
    }

    override fun afterEach(extensionContext: ExtensionContext) {
        SessionContextProvider.clear()
    }
}
