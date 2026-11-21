package app.myliberty.easy_records.cucumber.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import app.myliberty.easy_records.cucumber.support.TestUserRegistry
import app.myliberty.easy_records.support.TestConstants
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Instant
import java.util.UUID

class TestAuthenticationFilter : OncePerRequestFilter() {

  override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
    val token = request.getHeader(TestConstants.Tokens.TOKEN_HEADER)
    val systemUserId = mapTokenToUserId(token)
    if (systemUserId != null) {
      SecurityContextHolder.getContext().authentication = mapUserIdToAuthentication(systemUserId)
    }
    filterChain.doFilter(request, response)
  }

  private fun mapTokenToUserId(token: String?): UUID? {
    if (token.isNullOrBlank() || token == "null") return null
    return when (token) {
      TestConstants.Tokens.PLATFORM_ADMIN -> TestUserRegistry.platformAdminUserId
      TestConstants.Tokens.ORG_USER -> TestUserRegistry.organizationUserId
      else -> null
    }
  }

  private fun mapUserIdToAuthentication(systemUserId: UUID): JwtAuthenticationToken {
    val jwt = Jwt.withTokenValue(systemUserId.toString())
      .subject(systemUserId.toString())
      .header("alg", "none")
      .issuedAt(Instant.now())
      .expiresAt(Instant.now().plusSeconds(3600))
      .build()
    return JwtAuthenticationToken(jwt, emptyList())
  }
}
