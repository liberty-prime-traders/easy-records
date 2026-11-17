package app.myliberty.easy_records.organizations.business.accounting_config

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrgAccountingConfigRepository : JpaRepository<OrgAccountingConfigEntity, UUID>
