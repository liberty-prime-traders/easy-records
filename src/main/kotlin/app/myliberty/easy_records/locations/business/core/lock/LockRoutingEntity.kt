package app.myliberty.easy_records.locations.business.lock

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import app.myliberty.easy_records.util.model.TableNames
import java.util.UUID

// Exists only to give EntityAdvisoryLockRepository an entity to bind against, which routes its
// native advisory-lock query through the location-schema EntityManagerFactory. Maps only `id`
// on the `sale` table (always present) — never read, written, or otherwise used.
@Entity
@Table(name = TableNames.SALE)
class LockRoutingEntity(
    @Id
    var id: UUID
)
