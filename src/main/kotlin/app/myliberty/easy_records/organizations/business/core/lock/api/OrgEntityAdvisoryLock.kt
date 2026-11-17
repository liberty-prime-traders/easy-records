package app.myliberty.easy_records.organizations.business.lock.api

import app.myliberty.easy_records.organizations.business.lock.OrgEntityAdvisoryLockRepository
import app.myliberty.easy_records.util.business.lock.AdvisoryLock
import org.springframework.stereotype.Component

@Component
class OrgEntityAdvisoryLock(
    private val orgEntityAdvisoryLockRepository: OrgEntityAdvisoryLockRepository,
) {
    fun acquire(namespace: String, keys: Collection<String>) {
        AdvisoryLock.acquire(orgEntityAdvisoryLockRepository, namespace, keys)
    }

    fun acquire(namespace: String, key: String) {
        acquire(namespace, listOf(key))
    }
}
