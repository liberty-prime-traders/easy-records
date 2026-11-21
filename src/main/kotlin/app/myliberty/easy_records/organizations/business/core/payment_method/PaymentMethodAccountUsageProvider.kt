package app.myliberty.easy_records.organizations.business.core.payment_method

import app.myliberty.easy_records.organizations.business.core.account.api.AccountUsageProvider
import app.myliberty.easy_records.organizations.business.core.account.api.AccountUsageType
import org.springframework.stereotype.Component

@Component
class PaymentMethodAccountUsageProvider(
    private val paymentMethodCache: PaymentMethodCache
) : AccountUsageProvider {

    override val usageType = AccountUsageType.PAYMENT_METHOD

    override fun getReferences(accountCode: String): List<String> {
        return paymentMethodCache.getAllPaymentMethods()
            .filter { it.accountCode == accountCode }
            .map { "${it.referenceNumber} - ${it.name}" }
    }
}
