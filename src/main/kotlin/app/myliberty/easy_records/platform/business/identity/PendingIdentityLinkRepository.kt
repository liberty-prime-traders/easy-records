package app.myliberty.easy_records.platform.business.identity

import app.myliberty.easy_records.platform.business.identity.api.IdentityProvider
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PendingIdentityLinkRepository : JpaRepository<PendingIdentityLinkEntity, UUID> {
    fun findByUserIdAndProviderAndStatus(
        userId: UUID,
        provider: IdentityProvider,
        status: PendingLinkStatus
    ): List<PendingIdentityLinkEntity>
}
