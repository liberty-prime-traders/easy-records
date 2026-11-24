package app.myliberty.easy_records.cucumber.steps.organizations.core.settlement

import io.restassured.response.Response
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OpeningSettlementFixtureState {

  private var paymentMethodId: UUID? = null
  private var latestSettlementId: UUID? = null
  private var latestSettlementReference: String? = null

  fun rememberPaymentMethodId(newPaymentMethodId: UUID) {
    paymentMethodId = newPaymentMethodId
  }

  fun requirePaymentMethodId(): UUID =
    checkNotNull(paymentMethodId) { "No payment method was created for this scenario" }

  // The settlements list is newest first, so index 0 is the settlement the record call just saved.
  fun rememberLatestSettlement(settlementSummaryResponse: Response) {
    latestSettlementId = UUID.fromString(settlementSummaryResponse.jsonPath().getString("settlements[0].id"))
    latestSettlementReference = settlementSummaryResponse.jsonPath().getString("settlements[0].referenceNumber")
  }

  fun requireLatestSettlementId(): UUID =
    checkNotNull(latestSettlementId) { "No settlement was recorded for this scenario" }

  fun injectLatestSettlementReference(text: String): String {
    val reference = checkNotNull(latestSettlementReference) { "No settlement was recorded for this scenario" }
    return text.replace(SETTLEMENT_REFERENCE_PLACEHOLDER, reference)
  }

  companion object {
    private const val SETTLEMENT_REFERENCE_PLACEHOLDER = "#settlementReference"
  }
}
