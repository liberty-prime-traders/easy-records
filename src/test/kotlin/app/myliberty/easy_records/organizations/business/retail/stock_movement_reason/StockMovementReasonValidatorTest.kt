package app.myliberty.easy_records.organizations.business.retail.stock_movement_reason

import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.MovementType
import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.StockMovementReasonInsertDto
import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.StockMovementReasonUpdateDto
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class StockMovementReasonValidatorTest {

    private val cache = mock(StockMovementReasonCache::class.java)
    private val validator = StockMovementReasonValidator(cache)

    init {
        `when`(cache.getAll()).thenReturn(emptyList())
    }

    @Test
    fun `an org reason with no movement types is rejected`() {
        assertThrows(LibertyGenericException::class.java) {
            validator.validateInsert(StockMovementReasonInsertDto(name = "Samples", movementTypes = emptySet()))
        }
    }

    @Test
    fun `an org reason may apply to any movement types`() {
        validator.validateInsert(StockMovementReasonInsertDto(name = "Samples", movementTypes = MovementType.entries.toSet()))
    }

    @Test
    fun `clearing the movement types of an org reason is rejected`() {
        assertThrows(LibertyGenericException::class.java) {
            validator.validateUpdate(StockMovementReasonUpdateDto(id = UUID.randomUUID(), movementTypes = emptySet()))
        }
    }

    @Test
    fun `renaming a reason to the name it already has is accepted`() {
        val samplesReasonId = UUID.randomUUID()
        `when`(cache.getAll()).thenReturn(listOf(orgReasonDto(samplesReasonId, "Samples")))
        validator.validateUpdate(StockMovementReasonUpdateDto(id = samplesReasonId, name = Optional.of("Samples")))
    }

    private fun orgReasonDto(reasonId: UUID, reasonName: String) = StockMovementReasonDto(
        id = reasonId,
        referenceNumber = "SMR000001",
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.parse("2026-01-01T10:00:00Z"),
        name = reasonName,
        description = null,
        movementTypes = setOf(MovementType.SALE),
        systemDefined = false
    )
}
