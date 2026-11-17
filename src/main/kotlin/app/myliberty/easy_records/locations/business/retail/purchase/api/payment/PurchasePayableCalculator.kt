package app.myliberty.easy_records.locations.business.purchase.api.payment

import app.myliberty.easy_records.util.business.Decimals
import java.math.BigDecimal

object PurchasePayableCalculator {

  fun calculatePayable(paymentCeiling: BigDecimal, paidTotal: BigDecimal): BigDecimal =
    Decimals.MoneyScale.round(paymentCeiling - paidTotal).max(BigDecimal.ZERO)
}
