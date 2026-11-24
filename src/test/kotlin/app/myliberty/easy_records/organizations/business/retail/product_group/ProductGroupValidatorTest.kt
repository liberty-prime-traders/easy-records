package app.myliberty.easy_records.organizations.business.retail.product_group

import app.myliberty.easy_records.organizations.business.retail.product_category.api.ProductCategoryDto
import app.myliberty.easy_records.organizations.business.retail.product_category.api.ProductCategoryService
import app.myliberty.easy_records.organizations.business.retail.product_group.api.ProductGroupDto
import app.myliberty.easy_records.organizations.business.retail.product_group.api.ProductGroupInsertDto
import app.myliberty.easy_records.organizations.business.retail.product_group.api.ProductGroupUpdateDto
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

class ProductGroupValidatorTest {

    private lateinit var validator: ProductGroupValidator

    private val category = ProductCategoryDto(UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now(), "REF-C", "Dairy")
    private val milk = groupDto("Milk")
    private val cheese = groupDto("Cheese")

    @BeforeEach
    fun setUp() {
        val productGroupCache = mock(ProductGroupCache::class.java)
        val productCategoryService = mock(ProductCategoryService::class.java)
        `when`(productGroupCache.findAllProductGroups()).thenReturn(listOf(milk, cheese))
        `when`(productCategoryService.getAllCategoryDtos()).thenReturn(listOf(category))
        validator = ProductGroupValidator(productGroupCache, productCategoryService)
    }

    @Test
    fun `a new group needs a name`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            validator.validateProductGroupInsert(ProductGroupInsertDto(groupName = " ", categoryId = category.id))
        }

        assertEquals(ProductGroupValidator.NAME_IS_REQUIRED, exception.message)
    }

    @Test
    fun `a new group cannot repeat a name on letters and digits`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            validator.validateProductGroupInsert(ProductGroupInsertDto(groupName = "Mi_lk", categoryId = category.id))
        }

        assertEquals("A product group with the name Mi_lk already exists.", exception.message)
    }

    @Test
    fun `a group name without a letter or digit is rejected`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            validator.validateProductGroupInsert(ProductGroupInsertDto(groupName = "***", categoryId = category.id))
        }

        assertEquals(ProductGroupValidator.NAME_MUST_CONTAIN_LETTER_OR_DIGIT, exception.message)
    }

    @Test
    fun `a rename without a letter or digit is rejected`() {
        assertThrows(LibertyGenericException::class.java) {
            validator.validateProductGroupUpdate(ProductGroupUpdateDto(id = milk.id, groupName = Optional.of("---")))
        }
    }

    @Test
    fun `a new group must point at an existing category`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            validator.validateProductGroupInsert(ProductGroupInsertDto(groupName = "Yogurt", categoryId = UUID.randomUUID()))
        }

        assertEquals(ProductGroupValidator.INVALID_CATEGORY_ID, exception.message)
    }

    @Test
    fun `a valid new group passes`() {
        assertDoesNotThrow {
            validator.validateProductGroupInsert(ProductGroupInsertDto(groupName = "Yogurt", categoryId = category.id))
        }
    }

    // applyTo keeps omitted fields, so a description-only update must pass without a name or category.
    @Test
    fun `an update carrying only a description passes`() {
        assertDoesNotThrow {
            validator.validateProductGroupUpdate(ProductGroupUpdateDto(id = milk.id, description = Optional.of("Fresh")))
        }
    }

    @Test
    fun `an explicitly empty name is rejected on update`() {
        assertThrows(LibertyGenericException::class.java) {
            validator.validateProductGroupUpdate(ProductGroupUpdateDto(id = milk.id, groupName = Optional.empty()))
        }
    }

    @Test
    fun `a group cannot take the name of another group`() {
        assertThrows(LibertyGenericException::class.java) {
            validator.validateProductGroupUpdate(ProductGroupUpdateDto(id = cheese.id, groupName = Optional.of("milk")))
        }
    }

    @Test
    fun `a group can change the case of its own name`() {
        assertDoesNotThrow {
            validator.validateProductGroupUpdate(ProductGroupUpdateDto(id = milk.id, groupName = Optional.of("MILK")))
        }
    }

    @Test
    fun `an explicitly empty category is rejected on update`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            validator.validateProductGroupUpdate(ProductGroupUpdateDto(id = milk.id, categoryId = Optional.empty()))
        }

        assertEquals(ProductGroupValidator.CATEGORY_CANNOT_BE_EMPTY, exception.message)
    }

    @Test
    fun `a group cannot move to a category that does not exist`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            validator.validateProductGroupUpdate(ProductGroupUpdateDto(id = milk.id, categoryId = Optional.of(UUID.randomUUID())))
        }

        assertEquals(ProductGroupValidator.INVALID_CATEGORY_ID, exception.message)
    }

    private fun groupDto(name: String) = ProductGroupDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        referenceNumber = "REF-$name",
        groupName = name,
        categoryId = category.id
    )
}
