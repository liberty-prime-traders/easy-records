package app.myliberty.easy_records.platform.rest.endpoints

import app.myliberty.easy_records.platform.business.table_registry.api.TableRegistryResponseDto
import app.myliberty.easy_records.platform.business.table_registry.api.TableRegistryService
import app.myliberty.easy_records.platform.business.table_registry.api.TableRegistryUpdateDto
import app.myliberty.easy_records.util.enums.RtsRoleNames
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/table-registries")
@PreAuthorize("hasRole('${RtsRoleNames.PLATFORM_ADMIN}')")
class TableRegistryEndpoint(private val tableRegistryService: TableRegistryService) {

    @GetMapping
    fun getAll(): Collection<TableRegistryResponseDto> = tableRegistryService.getAll()

    @PutMapping("{id}/validate")
    fun validate(@PathVariable id: UUID): TableRegistryResponseDto = tableRegistryService.validate(id)

    @PostMapping("validate-all")
    fun validateAll(): Collection<TableRegistryResponseDto> = tableRegistryService.validateAll()

    @PutMapping
    fun update(@RequestBody dto: TableRegistryUpdateDto): TableRegistryResponseDto = tableRegistryService.update(dto)

}
