package app.myliberty.easy_records.organizations.rest.endpoints.core

import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import app.myliberty.easy_records.organizations.business.core.expense_type.api.ExpenseTypeInsertDto
import app.myliberty.easy_records.organizations.business.core.expense_type.api.ExpenseTypeRenameDto
import app.myliberty.easy_records.organizations.business.core.expense_type.api.ExpenseTypeResponseDto
import app.myliberty.easy_records.organizations.business.core.expense_type.api.ExpenseTypeService
import app.myliberty.easy_records.organizations.business.core.expense_type.api.ExpenseTypeUpdateDto
import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.util.annotations.RequiresFeature
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/expense-types")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class ExpenseTypeEndpoint(
    private val expenseTypeService: ExpenseTypeService
) {

    @GetMapping
    fun getAll(@RequestParam(required = false) sourceType: ExpenseSourceType?): List<ExpenseTypeResponseDto> =
        expenseTypeService.getAll(sourceType)

    @PostMapping
    fun create(@RequestBody expenseTypeInsertDto: ExpenseTypeInsertDto): ExpenseTypeResponseDto =
        expenseTypeService.create(expenseTypeInsertDto)

    @PutMapping
    fun update(@RequestBody expenseTypeUpdateDto: ExpenseTypeUpdateDto): ExpenseTypeResponseDto =
        expenseTypeService.update(expenseTypeUpdateDto)

    @PutMapping("name")
    fun rename(@RequestBody expenseTypeRenameDto: ExpenseTypeRenameDto): ExpenseTypeResponseDto =
        expenseTypeService.rename(expenseTypeRenameDto)
}
