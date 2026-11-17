package app.myliberty.easy_records.organizations.business.product

import app.myliberty.easy_records.configuration.datasource.DataSourceBeanNames
import app.myliberty.easy_records.organizations.business.product.api.OrganizationProductResponseDto
import app.myliberty.easy_records.organizations.business.unitvalue.api.UnitValueFetcher
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import app.myliberty.easy_records.util.queries.SqlSearchExecutor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Component

@Component
class OrganizationProductSearchExecutor(
  @Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_ENTITY_MANAGER_FACTORY)
  emf: LocalContainerEntityManagerFactoryBean,
  private val mapper: OrganizationProductMapper,
  private val unitValueFetcher: UnitValueFetcher
) : SqlSearchExecutor<OrganizationProductEntity, OrganizationProductResponseDto>(emf, OrganizationProductEntity::class.java) {

  override fun map(entities: List<OrganizationProductEntity>): List<OrganizationProductResponseDto> {
    val unitNamesById = unitValueFetcher.getUnitNamesById()
    return entities.map { entity ->
      val baseUnit = unitNamesById[entity.baseUnitId]
        ?: throw RtsGenericException("Unit name not found for id ${entity.baseUnitId}")
      mapper.toResponseDto(mapper.toDomainDto(entity), baseUnit)
    }
  }
}

