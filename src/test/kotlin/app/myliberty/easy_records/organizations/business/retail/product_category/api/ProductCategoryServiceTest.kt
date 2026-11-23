package app.myliberty.easy_records.organizations.business.retail.product_category.api

import app.myliberty.easy_records.organizations.business.retail.product_category.ProductCategoryCache
import app.myliberty.easy_records.organizations.business.retail.product_category.ProductCategoryMapper
import app.myliberty.easy_records.organizations.business.retail.product_group.api.ProductGroupDataFetcher
import app.myliberty.easy_records.support.anyNonNull
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import app.myliberty.easy_records.util.exceptions.UpdatingNonExistingRecordException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class ProductCategoryServiceTest {

    private lateinit var productCategoryCache: ProductCategoryCache
    private lateinit var productCategoryMapper: ProductCategoryMapper
    private lateinit var productGroupDataFetcher: ProductGroupDataFetcher
    private lateinit var service: ProductCategoryService

    private val dairy = categoryDto("Dairy", description = "Milk and cheese")
    private val bakery = categoryDto("Bakery")

    @BeforeEach
    fun setUp() {
        productCategoryCache = mock(ProductCategoryCache::class.java)
        productCategoryMapper = mock(ProductCategoryMapper::class.java)
        productGroupDataFetcher = mock(ProductGroupDataFetcher::class.java)
        service = ProductCategoryService(productCategoryMapper, productCategoryCache, productGroupDataFetcher)
        `when`(productCategoryCache.getAllCategories()).thenReturn(listOf(dairy, bakery))
    }

    @Test
    fun `a category without a name is rejected`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            service.createCategory(ProductCategoryInsertDto(categoryName = "   "))
        }

        assertEquals(ProductCategoryService.NAME_IS_REQUIRED, exception.message)
        verify(productCategoryCache, never()).create(anyNonNull(ProductCategoryInsertDto::class.java))
    }

    @Test
    fun `a category name that matches on letters and digits is a duplicate`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            service.createCategory(ProductCategoryInsertDto(categoryName = "Da-iry!"))
        }

        assertEquals("A category with the name Da-iry! already exists.", exception.message)
    }

    @Test
    fun `a category name without a letter or digit is rejected`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            service.createCategory(ProductCategoryInsertDto(categoryName = "***"))
        }

        assertEquals(ProductCategoryService.NAME_MUST_CONTAIN_LETTER_OR_DIGIT, exception.message)
    }

    @Test
    fun `a new category is saved and returned`() {
        val insert = ProductCategoryInsertDto(categoryName = "Drinks")
        val saved = categoryDto("Drinks")
        `when`(productCategoryCache.create(insert)).thenReturn(saved)
        `when`(productCategoryMapper.toResponseDto(saved)).thenReturn(responseDto(saved))

        assertEquals("Drinks", service.createCategory(insert).categoryName)
    }

    @Test
    fun `updating an unknown category is rejected`() {
        assertThrows(UpdatingNonExistingRecordException::class.java) {
            service.updateCategory(ProductCategoryUpdateDto(id = UUID.randomUUID(), categoryName = Optional.of("Drinks")))
        }
    }

    @Test
    fun `a category cannot take the name of another category`() {
        assertThrows(LibertyGenericException::class.java) {
            service.updateCategory(ProductCategoryUpdateDto(id = bakery.id, categoryName = Optional.of("dairy")))
        }
    }

    @Test
    fun `a category can change the case of its own name`() {
        val renamed = dairy.copy(categoryName = "DAIRY")
        stubSave(renamed)

        service.updateCategory(ProductCategoryUpdateDto(id = dairy.id, categoryName = Optional.of("DAIRY")))

        verify(productCategoryCache).save(renamed)
    }

    @Test
    fun `a rename without a letter or digit is rejected`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            service.updateCategory(ProductCategoryUpdateDto(id = dairy.id, categoryName = Optional.of("---")))
        }

        assertEquals(ProductCategoryService.NAME_MUST_CONTAIN_LETTER_OR_DIGIT, exception.message)
    }

    @Test
    fun `an explicitly empty name is rejected`() {
        val exception = assertThrows(LibertyGenericException::class.java) {
            service.updateCategory(ProductCategoryUpdateDto(id = dairy.id, categoryName = Optional.empty()))
        }

        assertEquals(ProductCategoryService.NAME_IS_REQUIRED, exception.message)
    }

    // applyTo keeps omitted fields, so a description-only update must pass without a name.
    @Test
    fun `an update carrying only a description keeps the name`() {
        val described = dairy.copy(description = "Fresh dairy")
        stubSave(described)

        service.updateCategory(ProductCategoryUpdateDto(id = dairy.id, description = Optional.of("Fresh dairy")))

        verify(productCategoryCache).save(described)
    }

    @Test
    fun `a category that groups still use cannot be deleted`() {
        `when`(productGroupDataFetcher.isCategoryInUse(dairy.id)).thenReturn(true)

        val exception = assertThrows(LibertyGenericException::class.java) { service.deleteCategory(dairy.id) }

        assertEquals(ProductCategoryService.CATEGORY_IN_USE, exception.message)
        verify(productCategoryCache, never()).deleteCategory(dairy.id)
    }

    private fun stubSave(saved: ProductCategoryDto) {
        `when`(productCategoryCache.save(saved)).thenReturn(saved)
        `when`(productCategoryMapper.toResponseDto(saved)).thenReturn(responseDto(saved))
    }

    private fun responseDto(dto: ProductCategoryDto) =
        ProductCategoryResponseDto(dto.id, dto.categoryName, dto.description, "Someone", dto.createdOn, dto.referenceNumber)

    private fun categoryDto(name: String, description: String? = null) = ProductCategoryDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        referenceNumber = "REF-$name",
        categoryName = name,
        description = description
    )
}
