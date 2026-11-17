package app.myliberty.easy_records.organizations.rest.endpoints

import app.myliberty.easy_records.organizations.business.core.account.api.AccountChildCreateRequest
import app.myliberty.easy_records.organizations.business.core.account.api.AccountResponseDto
import app.myliberty.easy_records.organizations.business.core.account.api.AccountRootCreateRequest
import app.myliberty.easy_records.organizations.business.core.account.api.AccountService
import app.myliberty.easy_records.organizations.business.core.account.api.AccountUpdateDto
import app.myliberty.easy_records.organizations.business.core.account_selection.api.AccountSelectionService
import app.myliberty.easy_records.organizations.business.core.account_selection.api.AccountsTreesForSelection
import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.util.annotations.RequiresFeature
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/accounts")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class AccountEndpoint(
    private val accountService: AccountService,
    private val accountSelectionService: AccountSelectionService
) {

    @GetMapping
    fun getAll(): List<AccountResponseDto> = accountService.getAll()

    @GetMapping("selection-trees")
    fun getTreesForSelection(): AccountsTreesForSelection = accountSelectionService.buildTreesForSelection()

    @PostMapping("child")
    fun createChild(@RequestBody dto: AccountChildCreateRequest): AccountResponseDto = accountService.createChild(dto)

    @PostMapping("root")
    fun createRoot(@RequestBody dto: AccountRootCreateRequest): AccountResponseDto = accountService.createRoot(dto)

    @PutMapping
    fun rename(@RequestBody dto: AccountUpdateDto): AccountResponseDto = accountService.rename(dto)

    @PutMapping("{id}/deactivate")
    fun deactivate(@PathVariable id: UUID): AccountResponseDto = accountService.toggleActive(id, false)

    @PutMapping("{id}/activate")
    fun activate(@PathVariable id: UUID): AccountResponseDto = accountService.toggleActive(id, true)
}
