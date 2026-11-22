package app.myliberty.easy_records.organizations.business.retail.unitvalue

import app.myliberty.easy_records.organizations.business.retail.unitgroup.api.UnitGroupDataFetcher
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitValueInsertDto
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitValueUpdateDto
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class UnitValueValidatorTest {

    private val weightGroupId = UUID.randomUUID()
    private val miscGroupId = UUID.randomUUID()
    private val volumeGroupId = UUID.randomUUID()

    private val gram = unit("Gram", "g", weightGroupId)
    private val kilogram = unit("Kilogram", "kg", weightGroupId, baseUnit = gram.id, unitsOfBasePerUnit = 1000L)
    private val tonne = unit("Tonne", "t", weightGroupId, baseUnit = kilogram.id, unitsOfBasePerUnit = 1000L)
    private val miscPiece = unit("Piece", "misc-pc", miscGroupId)
    private val millilitre = unit("Millilitre", "ml", volumeGroupId)

    private lateinit var unitValueCache: UnitValueCache
    private lateinit var unitGroupDataFetcher: UnitGroupDataFetcher
    private lateinit var validator: UnitValueValidator

    @BeforeEach
    fun setUp() {
        unitValueCache = mock(UnitValueCache::class.java)
        unitGroupDataFetcher = mock(UnitGroupDataFetcher::class.java)
        validator = UnitValueValidator(unitValueCache, unitGroupDataFetcher)

        val allUnits = listOf(gram, kilogram, tonne, miscPiece, millilitre)
        `when`(unitValueCache.getAllUnitValues()).thenReturn(allUnits)
        `when`(unitValueCache.getByUnitGroupId(weightGroupId)).thenReturn(listOf(gram, kilogram, tonne))
        `when`(unitValueCache.getByUnitGroupId(miscGroupId)).thenReturn(listOf(miscPiece))
        `when`(unitValueCache.getByUnitGroupId(volumeGroupId)).thenReturn(listOf(millilitre))
        `when`(unitGroupDataFetcher.exists(weightGroupId)).thenReturn(true)
        `when`(unitGroupDataFetcher.exists(miscGroupId)).thenReturn(true)
        `when`(unitGroupDataFetcher.exists(volumeGroupId)).thenReturn(true)
        `when`(unitGroupDataFetcher.isMiscellaneous(miscGroupId)).thenReturn(true)
    }

    @Test
    fun `an insert outside Miscellaneous without a base unit is rejected`() {
        assertRejected { validator.validateUnitValueInsert(insert("Pound", "lb", weightGroupId)) }
    }

    @Test
    fun `an insert with a base unit in the same group is accepted`() {
        validator.validateUnitValueInsert(insert("Pound", "lb", weightGroupId, baseUnit = gram.id, unitsOfBasePerUnit = 454L))
    }

    @Test
    fun `an insert whose base unit is in another group is rejected`() {
        assertRejected { validator.validateUnitValueInsert(insert("Pound", "lb", weightGroupId, baseUnit = millilitre.id, unitsOfBasePerUnit = 2L)) }
    }

    @Test
    fun `a Miscellaneous insert with a base unit is rejected and one without is accepted`() {
        assertRejected { validator.validateUnitValueInsert(insert("Carton", "ctn", miscGroupId, baseUnit = miscPiece.id, unitsOfBasePerUnit = 6L)) }
        validator.validateUnitValueInsert(insert("Carton", "ctn", miscGroupId))
    }

    @Test
    fun `a unit in another group cannot name a Miscellaneous unit as its base unit`() {
        assertRejected { validator.validateUnitValueInsert(insert("Pound", "lb", weightGroupId, baseUnit = miscPiece.id, unitsOfBasePerUnit = 2L)) }
    }

    @Test
    fun `an insert whose chain exceeds one billion roots is rejected`() {
        assertRejected { validator.validateUnitValueInsert(insert("Megatonne", "mt", weightGroupId, baseUnit = tonne.id, unitsOfBasePerUnit = 1001L)) }
    }

    @Test
    fun `names equal on alphanumerics are rejected within a group but allowed across groups`() {
        assertRejected { validator.validateUnitValueInsert(insert("kilo-gram", "kgm", weightGroupId, baseUnit = gram.id, unitsOfBasePerUnit = 1000L)) }
        validator.validateUnitValueInsert(insert("Gram", "g2", volumeGroupId, baseUnit = millilitre.id, unitsOfBasePerUnit = 5L))
    }

    @Test
    fun `a name with no letter or digit is rejected`() {
        assertRejected { validator.validateUnitValueInsert(insert("%%", "pct", weightGroupId, baseUnit = gram.id, unitsOfBasePerUnit = 2L)) }
    }

    @Test
    fun `a root cannot gain a base unit`() {
        assertRejected { validator.validateUnitValueUpdate(update(miscPiece, baseUnit = gram.id, unitsOfBasePerUnit = 2L)) }
        assertRejected { validator.validateUnitValueUpdate(update(gram, baseUnit = kilogram.id, unitsOfBasePerUnit = 2L)) }
    }

    @Test
    fun `an update that would create a cycle or a self reference is rejected`() {
        assertRejected { validator.validateUnitValueUpdate(update(kilogram, baseUnit = tonne.id, unitsOfBasePerUnit = 2L)) }
        assertRejected { validator.validateUnitValueUpdate(update(kilogram, baseUnit = kilogram.id, unitsOfBasePerUnit = 2L)) }
    }

    @Test
    fun `updating a mid-chain unit revalidates the units above it`() {
        assertRejected { validator.validateUnitValueUpdate(update(kilogram, baseUnit = gram.id, unitsOfBasePerUnit = 1_000_001L)) }
        validator.validateUnitValueUpdate(update(kilogram, baseUnit = gram.id, unitsOfBasePerUnit = 999_999L))
    }

    @Test
    fun `renaming a unit without touching its chain is accepted`() {
        validator.validateUnitValueUpdate(
            UnitValueUpdateDto(id = kilogram.id, name = Optional.of("Kilo"), code = Optional.of("kg"))
        )
    }

    @Test
    fun `an insert whose code exceeds the column length is rejected`() {
        assertRejected { validator.validateUnitValueInsert(insert("Pound", "a".repeat(26), weightGroupId, baseUnit = gram.id, unitsOfBasePerUnit = 454L)) }
        validator.validateUnitValueInsert(insert("Pound", "a".repeat(25), weightGroupId, baseUnit = gram.id, unitsOfBasePerUnit = 454L))
    }

    @Test
    fun `an insert using the piece reference marker as its code is rejected`() {
        assertRejected { validator.validateUnitValueInsert(insert("Pound", "__piece__", weightGroupId, baseUnit = gram.id, unitsOfBasePerUnit = 454L)) }
    }

    @Test
    fun `an update whose code exceeds the column length is rejected`() {
        assertRejected {
            validator.validateUnitValueUpdate(UnitValueUpdateDto(id = kilogram.id, name = Optional.of("Kilo"), code = Optional.of("a".repeat(26))))
        }
    }

    @Test
    fun `a root unit code that is too long, reserved or already taken is rejected`() {
        assertRejected { validator.validateRootUnitCode("a".repeat(26)) }
        assertRejected { validator.validateRootUnitCode("__piece__") }
        assertRejected { validator.validateRootUnitCode("kg") }
        validator.validateRootUnitCode("egg-root")
    }

    @Test
    fun `an insert missing its name, code or unit group is rejected`() {
        assertRejected { validator.validateUnitValueInsert(UnitValueInsertDto(code = "lb", unitGroupId = weightGroupId, baseUnit = gram.id, unitsOfBasePerUnit = 454L)) }
        assertRejected { validator.validateUnitValueInsert(insert("   ", "lb", weightGroupId, baseUnit = gram.id, unitsOfBasePerUnit = 454L)) }
        assertRejected { validator.validateUnitValueInsert(UnitValueInsertDto(name = "Pound", unitGroupId = weightGroupId, baseUnit = gram.id, unitsOfBasePerUnit = 454L)) }
        assertRejected { validator.validateUnitValueInsert(UnitValueInsertDto(name = "Pound", code = "lb", baseUnit = gram.id, unitsOfBasePerUnit = 454L)) }
    }

    @Test
    fun `an insert into a unit group that does not exist is rejected`() {
        assertRejected { validator.validateUnitValueInsert(insert("Pound", "lb", UUID.randomUUID(), baseUnit = gram.id, unitsOfBasePerUnit = 454L)) }
    }

    @Test
    fun `an insert reusing the code of a unit in another group is rejected`() {
        assertRejected { validator.validateUnitValueInsert(insert("Pound", "ml", weightGroupId, baseUnit = gram.id, unitsOfBasePerUnit = 454L)) }
    }

    @Test
    fun `an insert whose units of base per unit is below one is rejected`() {
        assertRejected { validator.validateUnitValueInsert(insert("Pound", "lb", weightGroupId, baseUnit = gram.id, unitsOfBasePerUnit = 0L)) }
        assertRejected { validator.validateUnitValueInsert(insert("Pound", "lb", weightGroupId, baseUnit = gram.id, unitsOfBasePerUnit = -5L)) }
    }

    @Test
    fun `an update that sets only one half of the base unit pair is rejected`() {
        assertRejected {
            validator.validateUnitValueUpdate(
                UnitValueUpdateDto(id = kilogram.id, name = Optional.of("Kilogram"), code = Optional.of("kg"), baseUnit = Optional.of(gram.id))
            )
        }
        assertRejected {
            validator.validateUnitValueUpdate(
                UnitValueUpdateDto(id = kilogram.id, name = Optional.of("Kilogram"), code = Optional.of("kg"), unitsOfBasePerUnit = Optional.of(500L))
            )
        }
    }

    @Test
    fun `an update cannot move a unit onto a base unit from another group`() {
        assertRejected { validator.validateUnitValueUpdate(update(kilogram, baseUnit = millilitre.id, unitsOfBasePerUnit = 2L)) }
    }

    @Test
    fun `an update cannot take another unit's name or code`() {
        assertRejected {
            validator.validateUnitValueUpdate(UnitValueUpdateDto(id = kilogram.id, name = Optional.of("gram"), code = Optional.of("kg")))
        }
        assertRejected {
            validator.validateUnitValueUpdate(UnitValueUpdateDto(id = kilogram.id, name = Optional.of("Kilogram"), code = Optional.of("g")))
        }
    }

    @Test
    fun `an update naming a unit that does not exist is rejected`() {
        assertRejected {
            validator.validateUnitValueUpdate(UnitValueUpdateDto(id = UUID.randomUUID(), name = Optional.of("Pound"), code = Optional.of("lb")))
        }
    }

    private fun assertRejected(action: () -> Unit) {
        assertThrows(LibertyGenericException::class.java, action)
    }

    private fun insert(name: String, code: String, unitGroupId: UUID, baseUnit: UUID? = null, unitsOfBasePerUnit: Long? = null) =
        UnitValueInsertDto(name = name, code = code, unitGroupId = unitGroupId, baseUnit = baseUnit, unitsOfBasePerUnit = unitsOfBasePerUnit)

    private fun update(existing: UnitValueDto, baseUnit: UUID, unitsOfBasePerUnit: Long) = UnitValueUpdateDto(
        id = existing.id,
        name = Optional.of(existing.name!!),
        code = Optional.of(existing.code!!),
        baseUnit = Optional.of(baseUnit),
        unitsOfBasePerUnit = Optional.of(unitsOfBasePerUnit)
    )

    private fun unit(name: String, code: String, unitGroupId: UUID, baseUnit: UUID? = null, unitsOfBasePerUnit: Long? = null) = UnitValueDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        referenceNumber = "REF-$code",
        name = name,
        code = code,
        unitGroupId = unitGroupId,
        baseUnit = baseUnit,
        unitsOfBasePerUnit = unitsOfBasePerUnit
    )
}
