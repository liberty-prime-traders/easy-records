package app.myliberty.easy_records.organizations.business.retail.product

import app.myliberty.easy_records.organizations.business.retail.product.api.OrganizationProductInsertDto
import app.myliberty.easy_records.organizations.business.retail.product.api.OrganizationProductUpdateDto
import app.myliberty.easy_records.organizations.business.retail.product_group.api.ProductGroupDto
import app.myliberty.easy_records.organizations.business.retail.product_group.api.ProductGroupService
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitValueFetcher
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class OrganizationProductValidatorTest {

    private lateinit var validator: OrganizationProductValidator

    private val group = ProductGroupDto(UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now(), "REF-G", "Milk", null, UUID.randomUUID())
    private val unitId = UUID.randomUUID()
    private val existingProductId = UUID.randomUUID()
    private val existingProduct = OrganizationProductEntity("Whole Milk", null, group.id, unitId).apply { id = existingProductId }

    @BeforeEach
    fun setUp() {
        val organizationProductRepository = mock(OrganizationProductRepository::class.java)
        val productGroupService = mock(ProductGroupService::class.java)
        val unitValueFetcher = mock(UnitValueFetcher::class.java)
        `when`(productGroupService.getAllGroupDtos()).thenReturn(listOf(group))
        `when`(unitValueFetcher.getUnitNamesById()).thenReturn(mapOf(unitId to "Piece"))
        `when`(organizationProductRepository.findFirstByNameKey("wholemilk")).thenReturn(existingProduct)
        validator = OrganizationProductValidator(organizationProductRepository, productGroupService, unitValueFetcher)
    }

    @Test
    fun `a new product needs a name`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            validator.validateProductInsert(OrganizationProductInsertDto("  ", null, group.id, unitId))
        }

        assertEquals(OrganizationProductValidator.NAME_IS_REQUIRED, exception.message)
    }

    @Test
    fun `a new product cannot repeat a name on letters and digits`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            validator.validateProductInsert(OrganizationProductInsertDto("Whole-Milk", null, group.id, unitId))
        }

        assertEquals("A product with the name Whole-Milk already exists.", exception.message)
    }

    @Test
    fun `a product name without a letter or digit is rejected`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            validator.validateProductInsert(OrganizationProductInsertDto("***", null, group.id, unitId))
        }

        assertEquals(OrganizationProductValidator.NAME_MUST_CONTAIN_LETTER_OR_DIGIT, exception.message)
    }

    @Test
    fun `a new product must point at an existing group`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            validator.validateProductInsert(OrganizationProductInsertDto("Skim Milk", null, UUID.randomUUID(), unitId))
        }

        assertEquals(OrganizationProductValidator.PRODUCT_GROUP_DOES_NOT_EXIST, exception.message)
    }

    @Test
    fun `a new product must point at an existing base unit`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            validator.validateProductInsert(OrganizationProductInsertDto("Skim Milk", null, group.id, UUID.randomUUID()))
        }

        assertEquals(OrganizationProductValidator.BASE_UNIT_DOES_NOT_EXIST, exception.message)
    }

    @Test
    fun `a valid new product passes`() {
        assertDoesNotThrow { validator.validateProductInsert(OrganizationProductInsertDto("Skim Milk", null, group.id, unitId)) }
    }

    // applyTo keeps omitted fields, so a description-only update must pass without a name or group.
    @Test
    fun `an update carrying only a description passes`() {
        assertDoesNotThrow {
            validator.validateProductUpdate(OrganizationProductUpdateDto(id = existingProductId, description = Optional.of("Fresh")))
        }
    }

    @Test
    fun `an explicitly empty name is rejected on update`() {
        assertThrows(LibertyGenericException::class.java) {
            validator.validateProductUpdate(OrganizationProductUpdateDto(id = existingProductId, productName = Optional.empty()))
        }
    }

    @Test
    fun `a product cannot take the name of another product`() {
        assertThrows(LibertyGenericException::class.java) {
            validator.validateProductUpdate(OrganizationProductUpdateDto(id = UUID.randomUUID(), productName = Optional.of("whole milk")))
        }
    }

    @Test
    fun `a product can change the case of its own name`() {
        assertDoesNotThrow {
            validator.validateProductUpdate(OrganizationProductUpdateDto(id = existingProductId, productName = Optional.of("whole milk")))
        }
    }

    @Test
    fun `an explicitly empty group is rejected on update`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            validator.validateProductUpdate(OrganizationProductUpdateDto(id = existingProductId, productGroupId = Optional.empty()))
        }

        assertEquals(OrganizationProductValidator.PRODUCT_GROUP_CANNOT_BE_EMPTY, exception.message)
    }

    @Test
    fun `a product cannot move to a group that does not exist`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            validator.validateProductUpdate(OrganizationProductUpdateDto(id = existingProductId, productGroupId = Optional.of(UUID.randomUUID())))
        }

        assertEquals(OrganizationProductValidator.PRODUCT_GROUP_DOES_NOT_EXIST, exception.message)
    }
}
