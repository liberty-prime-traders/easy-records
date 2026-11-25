package app.myliberty.easy_records.temp

import app.myliberty.easy_records.configuration.datasource.DataSourceBeanNames
import app.myliberty.easy_records.support.TestConstants
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
class LoanTestDatabase(
  @Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_DATA_SOURCE)
  organizationDataSource: DataSource
) {

  private val jdbcTemplate = JdbcTemplate(organizationDataSource)

  fun countLedgerGroupsOfType(sourceTypeCode: String): Int =
    requireNotNull(
      jdbcTemplate.queryForObject(
        "SELECT count(*) FROM \"${TestConstants.Seed.ORG_SCHEMA}\".ledger_entry_group WHERE source_type = ?",
        Int::class.java,
        sourceTypeCode
      )
    )
}
