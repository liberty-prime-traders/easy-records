package app.myliberty.easy_records.organizations.business.core.contact

import app.myliberty.easy_records.support.WithDefaultOrganizationSession
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactInsertDto
import app.myliberty.easy_records.organizations.business.core.contact.api.IdentityType
import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.business.mappers.MoneyQualifier
import app.myliberty.easy_records.util.business.mappers.StringQualifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@WithDefaultOrganizationSession
class ContactMapperMoneyTest {

    private val contactMapper = ContactMapperImpl(StringQualifier, MoneyQualifier)

    @Test
    fun `the credit limit reaches the entity as the typed value`() {
        val contactInsertDto = ContactInsertDto(
            contactTypes = emptySet(),
            identityType = IdentityType.INDIVIDUAL,
            firstName = "Wanjiku",
            creditLimit = Money.of(BigDecimal("150.50")),
        )
        assertEquals(BigDecimal("150.50"), contactMapper.toEntity(contactInsertDto).creditLimit)
    }
}
