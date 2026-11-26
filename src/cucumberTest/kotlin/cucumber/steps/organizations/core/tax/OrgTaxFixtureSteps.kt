package app.myliberty.easy_records.cucumber.steps.organizations.core.tax

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.cucumber.java.en.Given
import app.myliberty.easy_records.configuration.session.ServiceAccountContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.cucumber.fixtures.platform.TaxTypeFixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.TestUserRegistry
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.organizations.business.core.account.api.AccountChildCreateRequest
import app.myliberty.easy_records.organizations.business.core.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeInsertDto
import app.myliberty.easy_records.organizations.business.core.organization_admin.api.OrganizationAdminService
import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.platform.business.organization.OrganizationCache
import app.myliberty.easy_records.platform.business.tax_type.api.TaxApplicationLevel
import app.myliberty.easy_records.platform.business.tax_type.api.TaxCalculationMethod
import app.myliberty.easy_records.platform.business.tax_type.api.TaxRecoveryType
import app.myliberty.easy_records.platform.business.tax_type.api.TaxTrigger
import app.myliberty.easy_records.platform.business.tax_type.api.TaxTypeInsertDto
import app.myliberty.easy_records.util.enums.ServiceAccount
import java.util.UUID
import app.myliberty.easy_records.cucumber.support.context.TransientKey

class OrgTaxFixtureSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext,
  private val organizationCache: OrganizationCache,
  private val organizationAdminService: OrganizationAdminService,
  private val taxTypeFixtureBuilder: TaxTypeFixtureBuilder,
) {

  private val objectMapper = jacksonObjectMapper()

  @Given("tax configuration is active")
  fun activateTaxConfiguration() {
    val response = apiClient.post("/secured/org-features/activate", setOf(Feature.TAX_CONFIGURATION))
    check(response.statusCode == 200) { "Failed to activate tax configuration: ${response.asString()}" }
  }

  // Organization admin history is not a protected table, so the founder row is gone after each reset.
  @Given("the platform admin is an organization admin")
  fun makePlatformAdminOrganizationAdmin() {
    val platformAdminUserId = checkNotNull(TestUserRegistry.platformAdminUserId) {
      "Platform admin was not registered"
    }
    val organizationId = UUID.fromString(injectContext.inject("{{organization}}"))
    val organization = organizationCache.getAllOrganizations().first { it.id == organizationId }
    ServiceAccountContext.runWithServiceAccount<Unit>(ServiceAccount.RECORD_INITIALIZER) {
      SessionContextProvider.initOrganization(organization)
      try {
        organizationAdminService.registerFounder(platformAdminUserId)
      } finally {
        SessionContextProvider.clear()
      }
    }
  }

  @Given("a non-recoverable tax type named {string} exists")
  fun createNonRecoverableTaxType(taxTypeName: String) {
    val taxTypeId = taxTypeFixtureBuilder.create(
      TaxTypeInsertDto(
        name = taxTypeName,
        calculationMethod = TaxCalculationMethod.PERCENTAGE,
        taxRecoveryType = TaxRecoveryType.NON_RECOVERABLE,
        taxApplicationLevel = TaxApplicationLevel.LINE_ITEM,
        taxTriggers = setOf(TaxTrigger.SALE),
      ),
    )
    injectContext.storeNamed(TransientKey.TAX_TYPE, taxTypeName, taxTypeId)
  }

  @Given("the tax type {string} applies in the jurisdiction {string}")
  fun linkTaxTypeToJurisdiction(taxTypeName: String, jurisdictionName: String) {
    val jurisdictionId = injectContext.getNamed(TransientKey.JURISDICTION, jurisdictionName)
    val taxTypeId = injectContext.getNamed(TransientKey.TAX_TYPE, taxTypeName)
    putJurisdiction(mapOf("id" to jurisdictionId, "taxTypesToAddOrReactivate" to listOf(taxTypeId)))
    val taxTypeLabel = "$jurisdictionName - $taxTypeName"
    val linkId = linkIdFromAvailableTree(taxTypeLabel)
    injectContext.storeNamedText(TransientKey.JURISDICTION_TAX_TYPE, taxTypeLabel, linkId)
  }

  @Given("the tax type {string} is stopped in the jurisdiction {string}")
  fun stopTaxTypeInJurisdiction(taxTypeName: String, jurisdictionName: String) {
    val jurisdictionId = injectContext.getNamed(TransientKey.JURISDICTION, jurisdictionName)
    val taxTypeId = injectContext.getNamed(TransientKey.TAX_TYPE, taxTypeName)
    putJurisdiction(mapOf("id" to jurisdictionId, "taxTypesToDiscontinue" to listOf(taxTypeId)))
  }

  @Given("a payable tax account named {string} exists")
  fun createPayableTaxAccount(accountName: String) {
    val accountCode = createChildAccount(accountName, SystemAccount.TAX_PAYABLE.code)
    injectContext.storeNamedText(TransientKey.TAX_ACCOUNT, accountName, accountCode)
  }

  @Given("a recoverable tax account named {string} exists")
  fun createRecoverableTaxAccount(accountName: String) {
    val accountCode = createChildAccount(accountName, SystemAccount.TAX_RECOVERABLE.code)
    injectContext.storeNamedText(TransientKey.TAX_ACCOUNT, accountName, accountCode)
  }

  @Given("the organization has registered {string} with payable {string} and recoverable {string}")
  fun registerOrgTaxTypeWithRecoverable(taxLabel: String, payableAccountName: String, recoverableAccountName: String) {
    postOrgTaxType(taxLabel, payableAccountName, recoverableAccountName)
  }

  @Given("the organization has registered {string} with payable {string}")
  fun registerOrgTaxTypeWithoutRecoverable(taxLabel: String, payableAccountName: String) {
    postOrgTaxType(taxLabel, payableAccountName, recoverableAccountName = null)
  }

  private fun postOrgTaxType(taxLabel: String, payableAccountName: String, recoverableAccountName: String?) {
    val jurisdictionTaxTypeLabelValue = injectContext.getNamedText(TransientKey.JURISDICTION_TAX_TYPE, taxLabel)
    val jurisdictionTaxTypeId = UUID.fromString(jurisdictionTaxTypeLabelValue)
    val payableAccountCode = injectContext.getNamedText(TransientKey.TAX_ACCOUNT, payableAccountName)
    val recoverableAccountCode = recoverableAccountName?.let {
      injectContext.getNamedText(TransientKey.TAX_ACCOUNT, it)
    }
    val insertDto = OrgJurisdictionTaxTypeInsertDto(
      jurisdictionTaxTypeId = jurisdictionTaxTypeId,
      payableAccountCode = payableAccountCode,
      recoverableAccountCode = recoverableAccountCode,
    )
    val response = apiClient.post("/secured/org-tax-types", listOf(insertDto))
    check(response.statusCode == 200) { "Failed to register org tax type: ${response.asString()}" }
    injectContext.storeNamedText(TransientKey.ORG_TAX_TYPE, taxLabel, response.jsonPath().getString("[0].id"))
  }

  private fun createChildAccount(accountName: String, parentAccountCode: String): String {
    val response = apiClient.post(
      "/secured/accounts/child",
      AccountChildCreateRequest(parentAccountCode = parentAccountCode, name = accountName),
    )
    check(response.statusCode == 200) { "Failed to create tax account: ${response.asString()}" }
    return response.jsonPath().getString("code")
  }

  private fun putJurisdiction(payload: Map<String, Any>) {
    val response = apiClient.put("/secured/jurisdictions", payload)
    check(response.statusCode == 200) { "Failed to update jurisdiction tax types: ${response.asString()}" }
  }

  private fun linkIdFromAvailableTree(taxLabel: String): String {
    val availableTree = objectMapper.readTree(apiClient.get("/secured/org-tax-types/available").asString())
    return checkNotNull(keyOfNodeLabelled(availableTree, taxLabel)) {
      "No available jurisdiction tax type labelled \"$taxLabel\""
    }
  }

  private fun keyOfNodeLabelled(nodes: JsonNode, taxLabel: String): String? {
    for (node in nodes) {
      if (node.path("label").asText() == taxLabel) return node.path("key").asText()
      val keyInChildren = keyOfNodeLabelled(node.path("children"), taxLabel)
      if (keyInChildren != null) return keyInChildren
    }
    return null
  }
}
