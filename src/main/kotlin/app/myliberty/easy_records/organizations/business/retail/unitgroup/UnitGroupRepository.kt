package app.myliberty.easy_records.organizations.business.unitgroup

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UnitGroupRepository: JpaRepository<UnitGroupEntity, UUID>
