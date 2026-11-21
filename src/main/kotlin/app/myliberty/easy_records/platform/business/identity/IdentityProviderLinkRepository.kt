package app.myliberty.easy_records.platform.business.identity

import app.myliberty.easy_records.platform.business.identity.api.IdentityProvider
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface IdentityProviderLinkRepository : JpaRepository<IdentityProviderLinkEntity, UUID> {
    fun findByProviderAndExternalId(provider: IdentityProvider, externalId: String): IdentityProviderLinkEntity?
}
