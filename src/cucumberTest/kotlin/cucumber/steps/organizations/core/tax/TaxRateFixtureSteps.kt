package app.myliberty.easy_records.cucumber.steps.organizations.core.tax

import io.cucumber.java.en.Given
import app.myliberty.easy_records.cucumber.fixtures.organizations.core.tax.OrgTaxFixtureRegistry
import app.myliberty.easy_records.cucumber.fixtures.organizations.core.tax.TaxRateFixtureRegistry
import app.myliberty.easy_records.cucumber.fixtures.platform.PlatformTaxFixtureRegistry
import app.myliberty.easy_records.cucumber.fixtures.platform.PlatformTaxKind
import app.myliberty.easy_records.cucumber.fixtures.platform.TaxTypeFixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.platform.business.tax_type.api.TaxApplicationLevel
import app.myliberty.easy_records.platform.business.tax_type.api.TaxCalculationMethod
import app.myliberty.easy_records.platform.business.tax_type.api.TaxRecoveryType
import app.myliberty.easy_records.platform.business.tax_type.api.TaxTrigger
import app.myliberty.easy_records.platform.business.tax_type.api.TaxTypeInsertDto

class TaxRateFixtureSteps(
  private val apiClient: ApiClient,
  private val taxTypeFixtureBuilder: TaxTypeFixtureBuilder,
  private val platformTaxFixtureRegistry: PlatformTaxFixtureRegistry,
  private val orgTaxFixtureRegistry: OrgTaxFixtureRegistry,
  private val taxRateFixtureRegistry: TaxRateFixtureRegistry,
) {

  @Given("a fixed value tax type named {string} exists")
  fun createFixedValueTaxType(taxTypeName: String) {
    val taxTypeId = taxTypeFixtureBuilder.create(
      TaxTypeInsertDto(
        name = taxTypeName,
        calculationMethod = TaxCalculationMethod.FIXED_VALUE,
        taxRecoveryType = TaxRecoveryType.NON_RECOVERABLE,
        taxApplicationLevel = TaxApplicationLevel.LINE_ITEM,
        taxTriggers = setOf(TaxTrigger.SALE),
      ),
    )
    platformTaxFixtureRegistry.register(PlatformTaxKind.TAX_TYPE, taxTypeName, taxTypeId)
  }

  @Given("a transaction level tax type named {string} exists")
  fun createTransactionLevelTaxType(taxTypeName: String) {
    val taxTypeId = taxTypeFixtureBuilder.create(
      TaxTypeInsertDto(
        name = taxTypeName,
        calculationMethod = TaxCalculationMethod.PERCENTAGE,
        taxRecoveryType = TaxRecoveryType.NON_RECOVERABLE,
        taxApplicationLevel = TaxApplicationLevel.TRANSACTION,
        taxTriggers = setOf(TaxTrigger.SALE),
      ),
    )
    platformTaxFixtureRegistry.register(PlatformTaxKind.TAX_TYPE, taxTypeName, taxTypeId)
  }

  @Given("the tax rate {string} is created with payload:")
  fun createTaxRate(taxRateLabel: String, payload: String) {
    val resolvedPayload = taxRateFixtureRegistry.resolveTokens(orgTaxFixtureRegistry.resolveTokens(payload))
    val response = apiClient.post("/secured/tax-rates", resolvedPayload)
    check(response.statusCode == 200) { "Failed to create tax rate: ${response.asString()}" }
    taxRateFixtureRegistry.register(taxRateLabel, response.jsonPath().getString("id"))
  }
}
