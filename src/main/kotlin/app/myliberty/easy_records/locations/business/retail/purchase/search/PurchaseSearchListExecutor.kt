package app.myliberty.easy_records.locations.business.retail.purchase.search

import app.myliberty.easy_records.configuration.datasource.DataSourceBeanNames
import app.myliberty.easy_records.locations.business.retail.purchase.PurchaseEntity
import app.myliberty.easy_records.util.queries.SqlSearchExecutor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Component

@Component
class PurchaseSearchListExecutor(
  @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_ENTITY_MANAGER_FACTORY)
  emf: LocalContainerEntityManagerFactoryBean
) : SqlSearchExecutor<PurchaseEntity, PurchaseEntity>(emf, PurchaseEntity::class.java) {

  override fun map(entities: List<PurchaseEntity>): List<PurchaseEntity> = entities
}
