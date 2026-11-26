package app.myliberty.easy_records.cucumber.fixtures.platform

import app.myliberty.easy_records.cucumber.fixtures.FixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.platform.business.tax_type.api.TaxApplicationLevel
import app.myliberty.easy_records.platform.business.tax_type.api.TaxCalculationMethod
import app.myliberty.easy_records.platform.business.tax_type.api.TaxRecoveryType
import app.myliberty.easy_records.platform.business.tax_type.api.TaxTrigger
import app.myliberty.easy_records.platform.business.tax_type.api.TaxTypeInsertDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TaxTypeFixtureBuilder(
  injectContext: InjectContext,
  apiClient: ApiClient,
) : FixtureBuilder<TaxTypeInsertDto>(injectContext, apiClient) {

  override val endpoint = "/secured/tax-types"

  override fun defaultDto() = TaxTypeInsertDto(
    name = "Tax Type",
    calculationMethod = TaxCalculationMethod.PERCENTAGE,
    taxRecoveryType = TaxRecoveryType.RECOVERABLE,
    taxApplicationLevel = TaxApplicationLevel.LINE_ITEM,
    taxTriggers = setOf(TaxTrigger.SALE),
  )

  fun createNamed(name: String): UUID = create(defaultDto().copy(name = name))
}
