package app.myliberty.easy_records.platform.business.organization

import app.myliberty.easy_records.configuration.datasource.DataSourceBeanNames
import app.myliberty.easy_records.util.business.SchemaCreator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class OrganizationSchemaService(
    @param:Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_DATA_SOURCE)
    private val dataSource: DataSource
) {

    @Value("\${spring.datasource.organization.changelog}")
    private lateinit var changeLog: String

    fun createSchema(schemaName: String) {
        SchemaCreator.createSchema(schemaName, dataSource, changeLog)
    }

    fun dropSchema(schemaName: String) {
        SchemaCreator.dropSchema(schemaName, dataSource)
    }
}
