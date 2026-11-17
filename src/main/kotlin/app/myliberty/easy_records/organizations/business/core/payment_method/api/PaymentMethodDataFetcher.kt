package app.myliberty.easy_records.organizations.business.payment_method.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.account.api.AccountService
import app.myliberty.easy_records.organizations.business.payment_method.PaymentMethodCache
import app.myliberty.easy_records.organizations.business.payment_method.PaymentMethodMapper
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class PaymentMethodDataFetcher(
    private val paymentMethodMapper: PaymentMethodMapper,
    private val paymentMethodCache: PaymentMethodCache,
    private val accountService: AccountService
) {

    fun getAllPaymentMethods(): Collection<PaymentMethodResponseDto> {
        val accountNamesByCode = accountService.getAccountNamesByCode()
        return paymentMethodCache.getAllPaymentMethods().map {
            paymentMethodMapper.toResponseDto(it, accountNamesByCode[it.accountCode])
        }
    }

    fun findAccountCode(id: UUID): String {
        return findAccountCodesByIds(listOf(id)).getValue(id)
    }

    fun findAccountCodesByIds(ids: Collection<UUID>): Map<UUID, String> {
        val accountCodesById = paymentMethodCache.getAllPaymentMethods().associateBy({ it.id }, { it.accountCode })
        val missingIds = ids.filterNot { it in accountCodesById }
        if (missingIds.isNotEmpty()) {
            throw RtsGenericException("Payment methods not found: ${missingIds.joinToString()}")
        }
        return accountCodesById.filterKeys { it in ids }
    }

    fun getNamesById(): Map<UUID, String> =
        paymentMethodCache.getAllPaymentMethods().mapNotNull { it.name?.let { name -> it.id to name } }.toMap()
}
