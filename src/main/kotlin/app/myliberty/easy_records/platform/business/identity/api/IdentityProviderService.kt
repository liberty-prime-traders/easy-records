package app.myliberty.easy_records.platform.business.identity.api

interface IdentityProviderService {
    fun supports(provider: IdentityProvider): Boolean
    fun authenticate(credential: String): AuthenticatedIdentity
}
