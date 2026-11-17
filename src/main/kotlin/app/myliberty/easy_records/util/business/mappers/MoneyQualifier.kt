package app.myliberty.easy_records.util.business.mappers

import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.business.MoneyDisplay
import app.myliberty.easy_records.util.business.PositiveMoney
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
object MoneyQualifier {
    fun toBigDecimal(money: Money?): BigDecimal? = money?.toBigDecimal()

    fun toBigDecimal(positiveMoney: PositiveMoney?): BigDecimal? = positiveMoney?.toBigDecimal()

    fun toMoneyDisplay(amount: BigDecimal?): MoneyDisplay? = amount?.let { MoneyDisplay.of(it) }
}
