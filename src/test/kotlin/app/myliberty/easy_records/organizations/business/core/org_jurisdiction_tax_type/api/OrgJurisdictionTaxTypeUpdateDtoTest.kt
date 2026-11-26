package app.myliberty.easy_records.organizations.business.core.org_jurisdiction_tax_type.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class OrgJurisdictionTaxTypeUpdateDtoTest {

    private val existingRegistration = OrgJurisdictionTaxTypeDto(
        id = UUID.fromString("00000000-0000-0000-0000-0000000000a1"),
        createdById = UUID.fromString("00000000-0000-0000-0000-0000000000b1"),
        createdOn = OffsetDateTime.parse("2026-01-01T00:00:00Z"),
        referenceNumber = "OJ000001",
        jurisdictionTaxTypeId = UUID.fromString("00000000-0000-0000-0000-0000000000c1"),
        status = OrgJurisdictionTaxTypeStatus.ACTIVE,
        payableAccountCode = "002.002.001",
        recoverableAccountCode = "001.001.004.001",
    )

    @Test
    fun `omitted fields keep the stored values`() {
        val updateDto = OrgJurisdictionTaxTypeUpdateDto(id = existingRegistration.id)

        val applied = updateDto.applyTo(existingRegistration)

        assertEquals(OrgJurisdictionTaxTypeStatus.ACTIVE, applied.status)
        assertEquals("002.002.001", applied.payableAccountCode)
        assertEquals("001.001.004.001", applied.recoverableAccountCode)
    }

    @Test
    fun `a blank recoverable account clears the stored one`() {
        val updateDto = OrgJurisdictionTaxTypeUpdateDto(
            id = existingRegistration.id,
            recoverableAccountCode = Optional.of("   "),
        )

        val applied = updateDto.applyTo(existingRegistration)

        assertNull(applied.recoverableAccountCode)
    }

    @Test
    fun `a blank payable account keeps the stored one`() {
        val updateDto = OrgJurisdictionTaxTypeUpdateDto(id = existingRegistration.id, payableAccountCode = " ")

        val applied = updateDto.applyTo(existingRegistration)

        assertEquals("002.002.001", applied.payableAccountCode)
    }
}
