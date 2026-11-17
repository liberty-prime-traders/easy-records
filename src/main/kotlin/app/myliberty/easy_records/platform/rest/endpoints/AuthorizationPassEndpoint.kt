package app.myliberty.easy_records.platform.rest.endpoints

import app.myliberty.easy_records.platform.business.authorization_pass.api.AuthorizationPassInsertDto
import app.myliberty.easy_records.platform.business.authorization_pass.api.AuthorizationPassResponseDto
import app.myliberty.easy_records.platform.business.authorization_pass.api.AuthorizationPassService
import app.myliberty.easy_records.platform.business.authorization_pass.api.AuthorizationPassUpdateDto
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
@RequestMapping("secured/authorization-passes")
@PreAuthorize("hasRole('${RtsRoleNames.PLATFORM_ADMIN}')")
class AuthorizationPassEndpoint(private val authorizationPassService: AuthorizationPassService) {

    @GetMapping
    fun getAll(): List<AuthorizationPassResponseDto> = authorizationPassService.getAllPasses()

    @PostMapping
    fun issue(@RequestBody dto: AuthorizationPassInsertDto): AuthorizationPassResponseDto =
        authorizationPassService.issue(dto)

    @PutMapping
    fun update(@RequestBody dto: AuthorizationPassUpdateDto): AuthorizationPassResponseDto =
        authorizationPassService.update(dto)

    @GetMapping("/{id}/secret-code")
    fun getSecretCode(@PathVariable id: UUID): Map<String, UUID> =
        mapOf("code" to authorizationPassService.getSecretCode(id))

    @PutMapping("/{id}/revoke")
    fun revoke(@PathVariable id: UUID): AuthorizationPassResponseDto =
        authorizationPassService.revoke(id)
}
