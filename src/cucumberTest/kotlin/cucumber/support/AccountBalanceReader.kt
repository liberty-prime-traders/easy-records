package app.myliberty.easy_records.cucumber.support

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.stereotype.Component
import java.math.BigDecimal

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
}
