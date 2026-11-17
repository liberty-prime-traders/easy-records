package app.myliberty.easy_records.locations.business.lock.api

import app.myliberty.easy_records.locations.business.lock.EntityAdvisoryLockRepository
import app.myliberty.easy_records.util.business.lock.AdvisoryLock
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EntityAdvisoryLock(
    private val entityAdvisoryLockRepository: EntityAdvisoryLockRepository,
) {
    fun acquire(namespace: String, ids: Collection<UUID>) {
        AdvisoryLock.acquire(entityAdvisoryLockRepository, namespace, ids.map(UUID::toString))
    }

    fun acquire(namespace: String, id: UUID) {
        acquire(namespace, listOf(id))
    }
}
