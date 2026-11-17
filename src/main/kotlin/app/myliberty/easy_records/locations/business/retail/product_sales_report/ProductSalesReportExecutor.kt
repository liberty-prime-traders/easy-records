package app.myliberty.easy_records.locations.business.product_sales_report

import jakarta.persistence.Tuple
import app.myliberty.easy_records.configuration.datasource.DataSourceBeanNames
import app.myliberty.easy_records.util.queries.SqlSearchExecutor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Component

@Component
class ProductSalesReportExecutor(
  @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_ENTITY_MANAGER_FACTORY)
  emf: LocalContainerEntityManagerFactoryBean
) : SqlSearchExecutor<Tuple, Tuple>(emf, Tuple::class.java) {

  override fun map(entities: List<Tuple>): List<Tuple> = entities
}
