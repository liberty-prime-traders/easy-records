package app.myliberty.easy_records.platform.business.organization_join_request.api

import app.myliberty.easy_records.util.enums.HasCode

enum class JoinRequestStatus(override val code: String) : HasCode {
    PENDING("PND"),
    APPROVED("APRVD"),
    DENIED("DND"),
}
