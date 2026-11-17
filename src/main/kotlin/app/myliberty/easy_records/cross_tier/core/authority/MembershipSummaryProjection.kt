package app.myliberty.easy_records.cross_tier.core.authority

import java.util.UUID

interface MembershipSummaryProjection {
    fun getUserId(): UUID
    fun getMembershipCount(): Long
    fun getActive(): Boolean
}
