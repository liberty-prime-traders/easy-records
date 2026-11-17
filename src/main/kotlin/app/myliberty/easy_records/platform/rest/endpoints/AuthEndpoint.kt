package app.myliberty.easy_records.platform.rest.endpoints

import app.myliberty.easy_records.platform.business.identity.api.IdentityService
import app.myliberty.easy_records.platform.business.identity.api.LoginRequest
import app.myliberty.easy_records.platform.business.identity.api.LoginResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("auth")
class AuthEndpoint(private val identityService: IdentityService) {

    @PostMapping("login")
    fun login(@RequestBody loginRequest: LoginRequest): LoginResponse = identityService.login(loginRequest)
}
