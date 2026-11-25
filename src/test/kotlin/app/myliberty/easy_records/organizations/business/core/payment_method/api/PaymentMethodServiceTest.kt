package app.myliberty.easy_records.organizations.business.core.payment_method.api

import app.myliberty.easy_records.organizations.business.core.account.api.AccountService
import app.myliberty.easy_records.organizations.business.core.payment_method.PaymentAccountValidator
import app.myliberty.easy_records.organizations.business.core.payment_method.PaymentMethodCache
import app.myliberty.easy_records.organizations.business.core.payment_method.PaymentMethodMapper
import app.myliberty.easy_records.util.exceptions.DeletingNonExistingRecordException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.util.UUID

class PaymentMethodServiceTest {

    private val paymentMethodCache: PaymentMethodCache = mock(PaymentMethodCache::class.java)
    private val paymentMethodService = PaymentMethodService(
        mock(PaymentMethodMapper::class.java),
        paymentMethodCache,
        mock(PaymentAccountValidator::class.java),
        mock(AccountService::class.java)
    )

    @Test
    fun `deleting a payment method that does not exist is rejected without reaching the repository delete`() {
        val unknownPaymentMethodId = UUID.fromString("00000000-0000-0000-0000-000000000001")

        val exception = assertThrows(DeletingNonExistingRecordException::class.java) {
            paymentMethodService.deletePaymentMethod(unknownPaymentMethodId)
        }

        assertEquals("Attempted to Delete non-existent record", exception.message)
        verify(paymentMethodCache, never()).deletePaymentMethod(unknownPaymentMethodId)
    }
}
