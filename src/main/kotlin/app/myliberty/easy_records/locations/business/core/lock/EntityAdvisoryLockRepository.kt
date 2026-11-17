package app.myliberty.easy_records.locations.business.core.lock

import app.myliberty.easy_records.util.business.lock.AdvisoryLock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface EntityAdvisoryLockRepository : JpaRepository<LockRoutingEntity, UUID>, AdvisoryLock.Repository {

    @Query(
        value = "SELECT pg_advisory_xact_lock(hashtextextended(:key, 0))",
        nativeQuery = true
    )
    override fun acquire(key: String)
}
