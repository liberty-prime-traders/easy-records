package app.myliberty.easy_records.platform.business.feature

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FeatureRepository : JpaRepository<FeatureEntity, UUID>
