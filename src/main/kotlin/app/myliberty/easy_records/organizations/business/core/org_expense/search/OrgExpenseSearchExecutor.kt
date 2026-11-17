package app.myliberty.easy_records.organizations.business.core.org_expense.search

import jakarta.persistence.Tuple
import app.myliberty.easy_records.configuration.datasource.DataSourceBeanNames
import app.myliberty.easy_records.util.queries.SqlSearchExecutor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Component

@Component
class OrgExpenseSearchExecutor(
    @Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_ENTITY_MANAGER_FACTORY)
    entityManagerFactory: LocalContainerEntityManagerFactoryBean
) : SqlSearchExecutor<Tuple, Tuple>(entityManagerFactory, Tuple::class.java) {

    override fun map(entities: List<Tuple>): List<Tuple> = entities
}
