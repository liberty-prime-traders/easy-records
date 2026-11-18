package app.myliberty.easy_records.organizations.business.core.jobtitle

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JobTitleRepository: JpaRepository<JobTitleEntity, UUID>
