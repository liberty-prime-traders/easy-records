package app.myliberty.easy_records.cucumber.support

import app.myliberty.easy_records.organizations.business.core.account.api.AccountType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.stereotype.Component
import java.math.BigDecimal

data class AccountBalanceRow(
  val code: String,
  val name: String,
  val accountType: AccountType,
  val parentAccountCode: String?,
  val currentBalance: BigDecimal
)

@Component
class AccountBalanceReader(
  private val apiClient: ApiClient
) {

  private val objectMapper = jacksonObjectMapper()

  fun balanceOf(accountCode: String): BigDecimal {
    val accountsJson = objectMapper.readTree(apiClient.get("/secured/accounts").asString())
    val accountNodes = accountsJson.findParents("code")
    val accountNode = accountNodes.firstOrNull { accountNode -> accountNode.path("code").asText() == accountCode }
    return BigDecimal(checkNotNull(accountNode) { "No account with code $accountCode" }.path("currentBalance").asText())
  }

  fun allAccounts(): List<AccountBalanceRow> {
    val accountsResponse = apiClient.get("/secured/accounts")
    check(accountsResponse.statusCode == 200) { "GET /secured/accounts answered ${accountsResponse.statusCode}" }
    return objectMapper.readTree(accountsResponse.asString()).map { accountNode ->
      AccountBalanceRow(
        code = accountNode.path("code").asText(),
        name = accountNode.path("name").asText(),
        accountType = AccountType.valueOf(accountNode.path("accountType").asText()),
        parentAccountCode = accountNode.path("parentAccountCode").takeUnless { it.isNull }?.asText(),
        currentBalance = BigDecimal(accountNode.path("currentBalance").asText())
      )
    }
  }
}
