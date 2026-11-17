package app.myliberty.easy_records.platform.rest.endpoints

import app.myliberty.easy_records.platform.business.reserved_subdomain.api.ReservedSubdomainDto
import app.myliberty.easy_records.platform.business.reserved_subdomain.api.ReservedSubdomainService
import app.myliberty.easy_records.util.enums.RtsPermissionNames
import app.myliberty.easy_records.util.enums.RtsRoleNames
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("secured/reserved-subdomains")
class ReservedSubdomainEndpoint(private val reservedSubdomainService: ReservedSubdomainService) {

    @GetMapping("verify")
    @PreAuthorize("hasAuthority('${RtsPermissionNames.CREATE_ORGANIZATION}')")
    fun sanitizeThenReserveSubdomain(@RequestParam("suggestedSubdomain") suggestedSubdomain: String): ReservedSubdomainDto =
        reservedSubdomainService.sanitizeThenReserveSubdomain(suggestedSubdomain)

    @GetMapping
    @PreAuthorize("hasRole('${RtsRoleNames.PLATFORM_ADMIN}')")
    fun getAllReservedSubdomains(): Collection<ReservedSubdomainDto> = reservedSubdomainService.getReservedSubdomains()

    @DeleteMapping("{id}")
    @PreAuthorize("hasRole('${RtsRoleNames.PLATFORM_ADMIN}')")
    fun releaseSubdomain(@PathVariable id: UUID): ResponseEntity<HttpStatus> {
        reservedSubdomainService.releaseSubdomain(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
