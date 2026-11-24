package app.myliberty.easy_records.cucumber.fixtures.organizations.retail

import app.myliberty.easy_records.cucumber.fixtures.FixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.MovementType
import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.StockMovementReasonInsertDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class StockMovementReasonFixtureBuilder(
  injectContext: InjectContext,
  apiClient: ApiClient
) : FixtureBuilder<StockMovementReasonInsertDto>(injectContext, apiClient) {

  override val endpoint = "/secured/stock-movement-reasons"

  override fun defaultDto() = StockMovementReasonInsertDto(
    name = "Test Reason",
    movementTypes = setOf(MovementType.STOCK_ADJUSTMENT_DECREASE)
  )

  fun createNamed(name: String): UUID = create(defaultDto().copy(name = name))
}
