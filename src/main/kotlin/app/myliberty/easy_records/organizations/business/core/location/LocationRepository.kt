package app.myliberty.easy_records.organizations.business.core.location

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LocationRepository : JpaRepository<LocationEntity, UUID>
