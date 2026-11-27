package app.myliberty.easy_records.util.quantity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class QuantityBreakdownLevelsTest {

  private val bottleId = UUID.randomUUID()
  private val sixPackId = UUID.randomUUID()
  private val crateId = UUID.randomUUID()
  private val palletId = UUID.randomUUID()
  private val createdOn = OffsetDateTime.parse("2030-01-01T00:00:00Z")

  private val chainUnits = listOf(
    BreakdownUnit(bottleId, "Bottle", 1, bottleId, createdOn),
    BreakdownUnit(sixPackId, "Six-pack", 6, bottleId, createdOn),
    BreakdownUnit(crateId, "Crate", 24, bottleId, createdOn),
    BreakdownUnit(palletId, "Pallet", 240, bottleId, createdOn)
  )
  private val palletBreakdown = QuantityBreakdown.forBaseUnit(chainUnits, palletId)

  @Test
  fun `a quantity that uses all four levels names each of them`() {
    assertEquals("1 Pallet 1 Crate 1 Six-pack 5 Bottles", render(palletBreakdown, "275"))
  }

  @Test
  fun `a level with nothing in it is left out`() {
    assertEquals("1 Pallet 1 Crate 1 Six-pack", render(palletBreakdown, "270"))
    assertEquals("1 Pallet 5 Bottles", render(palletBreakdown, "245"))
    assertEquals("1 Pallet 1 Crate", render(palletBreakdown, "264"))
  }

  @Test
  fun `a count of two or more is plural on every level and a count of one is singular`() {
    assertEquals("2 Pallets 2 Crates 2 Six-packs 2 Bottles", render(palletBreakdown, "542"))
    assertEquals("1 Pallet", render(palletBreakdown, "240"))
  }

  @Test
  fun `a zero quantity is shown in the base unit's plural`() {
    assertEquals("0 Pallets", render(palletBreakdown, "0"))
  }

  @Test
  fun `a quantity below the smallest level is shown in the root unit alone`() {
    assertEquals("5 Bottles", render(palletBreakdown, "5"))
    assertEquals("1 Bottle", render(palletBreakdown, "1"))
  }

  @Test
  fun `levels above the product's base unit are not used`() {
    val crateBreakdown = QuantityBreakdown.forBaseUnit(chainUnits, crateId)

    assertEquals("11 Crates 1 Six-pack 5 Bottles", render(crateBreakdown, "275"))
  }

  @Test
  fun `a base unit of six-packs shows only six-packs and bottles`() {
    val sixPackBreakdown = QuantityBreakdown.forBaseUnit(chainUnits, sixPackId)

    assertEquals("45 Six-packs 5 Bottles", render(sixPackBreakdown, "275"))
  }

  @Test
  fun `two units of the same size give the quantity to the one created first`() {
    val dozenId = UUID.randomUUID()
    val laterDozenId = UUID.randomUUID()
    val sameSizeUnits = listOf(
      BreakdownUnit(bottleId, "Bottle", 1, bottleId, createdOn),
      BreakdownUnit(dozenId, "Dozen", 12, bottleId, createdOn),
      BreakdownUnit(laterDozenId, "Doz", 12, bottleId, createdOn.plusMinutes(1)),
      BreakdownUnit(palletId, "Pallet", 240, bottleId, createdOn)
    )
    val breakdown = QuantityBreakdown.forBaseUnit(sameSizeUnits, palletId)

    assertEquals("2 Dozens 1 Bottle", render(breakdown, "25"))
  }

  @Test
  fun `a unit of the base unit's size created before it is skipped and the base unit takes the quantity`() {
    val dozenId = UUID.randomUUID()
    val earlierDozenId = UUID.randomUUID()
    val sameSizeUnits = listOf(
      BreakdownUnit(bottleId, "Bottle", 1, bottleId, createdOn),
      BreakdownUnit(earlierDozenId, "Doz", 12, bottleId, createdOn.minusMinutes(1)),
      BreakdownUnit(dozenId, "Dozen", 12, bottleId, createdOn)
    )
    val breakdown = QuantityBreakdown.forBaseUnit(sameSizeUnits, dozenId)

    assertEquals("2 Dozens 1 Bottle", render(breakdown, "25"))
  }

  @Test
  fun `a non-root unit the size of the root is skipped because the root carries the remainder`() {
    val dozenId = UUID.randomUUID()
    val eachId = UUID.randomUUID()
    val units = listOf(
      BreakdownUnit(bottleId, "Bottle", 1, bottleId, createdOn),
      BreakdownUnit(eachId, "Each", 1, bottleId, createdOn),
      BreakdownUnit(dozenId, "Dozen", 12, bottleId, createdOn)
    )
    val breakdown = QuantityBreakdown.forBaseUnit(units, dozenId)

    assertEquals("1 Dozen 1 Bottle", render(breakdown, "13"))
  }

  private fun render(quantityBreakdown: QuantityBreakdown, storageQuantity: String): String =
    quantityBreakdown.render(ExactQuantity.ofStorage(BigDecimal(storageQuantity)))
}
