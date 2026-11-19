package app.myliberty.easy_records.cucumber.support.assertions

import app.myliberty.easy_records.organizations.business.core.contact.ContactMapper
import app.myliberty.easy_records.organizations.business.core.contact.ContactRepository
import app.myliberty.easy_records.organizations.business.core.jobtitle.JobTitleMapper
import app.myliberty.easy_records.organizations.business.core.jobtitle.JobTitleRepository
import app.myliberty.easy_records.organizations.business.retail.product_category.ProductCategoryMapper
import app.myliberty.easy_records.organizations.business.retail.product_category.ProductCategoryRepository
import app.myliberty.easy_records.organizations.business.retail.unitgroup.UnitGroupMapper
import app.myliberty.easy_records.organizations.business.retail.unitgroup.UnitGroupRepository
import app.myliberty.easy_records.organizations.business.retail.unitvalue.UnitValueMapper
import app.myliberty.easy_records.organizations.business.retail.unitvalue.UnitValueRepository
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitValueFetcher
import app.myliberty.easy_records.platform.business.authorization_pass.AuthorizationPassMapper
import app.myliberty.easy_records.platform.business.authorization_pass.AuthorizationPassRepository
import app.myliberty.easy_records.platform.business.organization.OrganizationMapper
import app.myliberty.easy_records.platform.business.organization.OrganizationRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OrganizationPersistedResponseBinding(
  private val repository: OrganizationRepository,
  private val mapper: OrganizationMapper,
) : PersistedResponseBinding {
  override val alias = "organization"
  override val scope = SchemaScope.PLATFORM

  override fun responseDtoFor(id: UUID): Any {
    val entity = requireNotNull(repository.findByIdOrNull(id)) { "Organization '$id' not found" }
    return mapper.toResponseDto(mapper.toDomainDto(entity))
  }
}

@Component
class UnitGroupPersistedResponseBinding(
  private val repository: UnitGroupRepository,
  private val mapper: UnitGroupMapper,
) : PersistedResponseBinding {
  override val alias = "unitGroup"
  override val scope = SchemaScope.ORGANIZATION

  override fun responseDtoFor(id: UUID): Any {
    val entity = requireNotNull(repository.findByIdOrNull(id)) { "Unit group '$id' not found" }
    return mapper.toResponseDto(mapper.toDomainDto(entity))
  }
}

@Component
class UnitValuePersistedResponseBinding(
  private val repository: UnitValueRepository,
  private val mapper: UnitValueMapper,
  private val unitValueFetcher: UnitValueFetcher,
) : PersistedResponseBinding {
  override val alias = "unitValue"
  override val scope = SchemaScope.ORGANIZATION

  override fun responseDtoFor(id: UUID): Any {
    val entity = requireNotNull(repository.findByIdOrNull(id)) { "Unit value '$id' not found" }
    val dto = mapper.toDomainDto(entity)
    return mapper.toResponseDto(
      dto, unitValueFetcher.getUnitNamesById().getValue(dto.baseUnit!!)
    )
  }
}

@Component
class ProductCategoryPersistedResponseBinding(
  private val repository: ProductCategoryRepository,
  private val mapper: ProductCategoryMapper,
) : PersistedResponseBinding {
  override val alias = "category"
  override val scope = SchemaScope.ORGANIZATION

  override fun responseDtoFor(id: UUID): Any {
    val entity = requireNotNull(repository.findByIdOrNull(id)) { "Category '$id' not found" }
    return mapper.toResponseDto(mapper.toDomainDto(entity))
  }
}

@Component
class JobTitlePersistedResponseBinding(
  private val repository: JobTitleRepository,
  private val mapper: JobTitleMapper,
) : PersistedResponseBinding {
  override val alias = "jobTitle"
  override val scope = SchemaScope.ORGANIZATION

  override fun responseDtoFor(id: UUID): Any {
    val entity = requireNotNull(repository.findByIdOrNull(id)) { "Job title '$id' not found" }
    return mapper.toDto(mapper.toDomainDto(entity))
  }
}

@Component
class ContactPersistedResponseBinding(
  private val repository: ContactRepository,
  private val mapper: ContactMapper,
) : PersistedResponseBinding {
  override val alias = "contact"
  override val scope = SchemaScope.ORGANIZATION

  override fun responseDtoFor(id: UUID): Any {
    val entity = requireNotNull(repository.findByIdOrNull(id)) { "Contact '$id' not found" }
    return mapper. .toResponseDto(mapper.toDomainDto(entity))
  }
}

@Component
class AuthorizationPassPersistedResponseBinding(
  private val repository: AuthorizationPassRepository,
  private val mapper: AuthorizationPassMapper,
) : PersistedResponseBinding {
  override val alias = "authorizationPass"
  override val scope = SchemaScope.PLATFORM

  override fun responseDtoFor(id: UUID): Any {
    val entity = requireNotNull(repository.findByIdOrNull(id)) { "Authorization pass '$id' not found" }
    return mapper.toResponseDto(entity)
  }
}
