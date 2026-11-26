package app.myliberty.easy_records.organizations.business.core.payment_method.api

import app.myliberty.easy_records.organizations.business.core.payment_method.PaymentMethodDto
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class PaymentMethodUpdateDtoTest {

    private val existingPaymentMethod = PaymentMethodDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.parse("2026-03-01T10:00:00Z"),
        referenceNumber = "PYMT000001",
        name = "Till",
        description = "Counter cash",
        accountCode = "001.001.001"
    )

    @Test
    fun `an update that omits the account and description keeps the stored values`() {
        val paymentMethodUpdateDto = PaymentMethodUpdateDto(
            id = existingPaymentMethod.id,
            name = Optional.of("Counter")
        )

        val updated = paymentMethodUpdateDto.applyTo(existingPaymentMethod)

        assertEquals("Counter", updated.name)
        assertEquals("Counter cash", updated.description)
        assertEquals("001.001.001", updated.accountCode)
    }

    @Test
    fun `a provided name is trimmed before it is stored`() {
        val paymentMethodUpdateDto = PaymentMethodUpdateDto(
            id = existingPaymentMethod.id,
            name = Optional.of("  Counter  ")
        )

        val updated = paymentMethodUpdateDto.applyTo(existingPaymentMethod)

        assertEquals("Counter", updated.name)
    }

    @Test
    fun `an empty description optional clears the stored description`() {
        val paymentMethodUpdateDto = PaymentMethodUpdateDto(
            id = existingPaymentMethod.id,
            name = Optional.of("Till"),
            description = Optional.empty()
        )

        val updated = paymentMethodUpdateDto.applyTo(existingPaymentMethod)

        assertNull(updated.description)
    }

    @Test
    fun `a blank account code is rejected rather than cleared`() {
        val paymentMethodUpdateDto = PaymentMethodUpdateDto(
            id = existingPaymentMethod.id,
            name = Optional.of("Till"),
            accountCode = Optional.of("   ")
        )

        val thrown = assertThrows(LibertyGenericException::class.java) {
            paymentMethodUpdateDto.applyTo(existingPaymentMethod)
        }

        assertEquals("A Payment Method must have an account", thrown.message)
    }
}
