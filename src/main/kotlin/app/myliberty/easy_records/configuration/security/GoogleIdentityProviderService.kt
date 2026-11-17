package app.myliberty.easy_records.configuration.security

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import app.myliberty.easy_records.platform.business.identity.api.AuthenticatedIdentity
import app.myliberty.easy_records.platform.business.identity.api.IdentityProvider
import app.myliberty.easy_records.platform.business.identity.api.IdentityProviderService
import app.myliberty.easy_records.util.exceptions.AuthException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class GoogleIdentityProviderService(
    @param:Value($$"${google.oauth.client-id}")
    private val googleOauthClientId: String
) : IdentityProviderService {

    private val googleIdTokenVerifier: GoogleIdTokenVerifier =
        GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(listOf(googleOauthClientId))
            .build()

    override fun supports(provider: IdentityProvider): Boolean = provider == IdentityProvider.GOOGLE

    override fun authenticate(credential: String): AuthenticatedIdentity {
        val googleIdToken = try {
            googleIdTokenVerifier.verify(credential)
        } catch (_: Exception) {
            throw AuthException.InvalidCredential()
        } ?: throw AuthException.InvalidCredential()

        val payload = googleIdToken.payload
        return AuthenticatedIdentity(
            provider = IdentityProvider.GOOGLE,
            externalId = payload.subject,
            email = payload.email,
            firstName = resolveFirstName(payload["given_name"] as? String, payload.email),
            lastName = payload["family_name"] as? String
        )
    }

    private fun resolveFirstName(givenName: String?, email: String?): String =
        givenName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Google User"
}
