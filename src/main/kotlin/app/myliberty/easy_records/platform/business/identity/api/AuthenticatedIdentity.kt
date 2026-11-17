package app.myliberty.easy_records.platform.business.identity.api

data class AuthenticatedIdentity(
    val provider: IdentityProvider,
    val externalId: String,
    val email: String?,
    val firstName: String,
    val lastName: String?
)
