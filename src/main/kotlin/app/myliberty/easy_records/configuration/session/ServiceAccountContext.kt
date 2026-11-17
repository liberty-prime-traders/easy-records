package app.myliberty.easy_records.configuration.session

import app.myliberty.easy_records.util.enums.ServiceAccount

object ServiceAccountContext {

    fun <T> runWithServiceAccount(serviceAccount: ServiceAccount, block: () -> T): T {
        val sessionSnapshot = SessionContextProvider.getSession().copy()
        SessionContextProvider.initSystemUser(serviceAccount.uniqueId)
        return try {
            block()
        } finally {
            SessionContextProvider.setSession(sessionSnapshot)
        }
    }

    fun runWithServiceAccount(serviceAccount: ServiceAccount, block: () -> Unit) {
        runWithServiceAccount<Unit>(serviceAccount) { block() }
    }
}
