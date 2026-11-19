package app.myliberty.easy_records.locations.business.retail.location_product

import app.myliberty.easy_records.configuration.datasource.DataSourceBeanNames
import app.myliberty.easy_records.util.queries.SqlSearchExecutor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Component

@Component
class LocationProductDtoSearchExecutor(
    @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_ENTITY_MANAGER_FACTORY)
    emf: LocalContainerEntityManagerFactoryBean,
    private val locationProductMapper: LocationProductMapper,
) : SqlSearchExecutor<LocationProductEntity, LocationProductDto>(emf, LocationProductEntity::class.java) {

    override fun map(entities: List<LocationProductEntity>): List<LocationProductDto> =
        entities.map { locationProductMapper.toDomainDto(it) }
}
