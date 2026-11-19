package app.myliberty.easy_records.locations.business.retail.sale

import app.myliberty.easy_records.locations.business.retail.sale.api.SaleStatus
import java.math.BigDecimal

object SaleArrears {

    fun arrearsTotal(status: SaleStatus, receivableTotal: BigDecimal, totalPaid: BigDecimal): BigDecimal =
        if (status == SaleStatus.CONFIRMED) receivableTotal - totalPaid else BigDecimal.ZERO
}
