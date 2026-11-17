package app.myliberty.easy_records.locations.business.retail.sale_session.api

import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import java.util.UUID

data class SessionIdentity(
    val id: UUID? = null,
    val transientId: UUID? = null,
) {

    init {
        if ((id == null) == (transientId == null)) {
            throw LibertyGenericException("SessionIdentity requires exactly one of id or transientId")
        }
    }

    fun key(): UUID = id ?: transientId!!

    fun isPersisted(): Boolean = id != null

    companion object {
        fun mintFreshIdentity(): SessionIdentity = SessionIdentity(transientId = UUID.randomUUID())
        fun persisted(id: UUID): SessionIdentity = SessionIdentity(id = id)
    }
}
