package app.myliberty.easy_records.locations.business.retail.stock_adjustment.search

import app.myliberty.easy_records.configuration.datasource.DataSourceBeanNames
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.StockAdjustmentEntity
import app.myliberty.easy_records.util.queries.SqlSearchExecutor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Component

@Component
class StockAdjustmentSearchListExecutor(
    @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_ENTITY_MANAGER_FACTORY)
    entityManagerFactory: LocalContainerEntityManagerFactoryBean
) : SqlSearchExecutor<StockAdjustmentEntity, StockAdjustmentEntity>(
    entityManagerFactory, StockAdjustmentEntity::class.java
) {

    override fun map(entities: List<StockAdjustmentEntity>): List<StockAdjustmentEntity> = entities
}
