package app.myliberty.easy_records.organizations.business.retail.unit_bulk_import

import app.myliberty.easy_records.util.business.StringUtils

// The keys StringUtils.isEquivalent and isEquivalentOnAlphanumerics compare, so a set lookup matches what a scan would.
internal object BulkUnitNameKeys {

    fun groupKey(groupName: String?): String =
        if (groupName.isNullOrBlank()) "" else StringUtils.normalizeForComparison(groupName)

    fun alphanumericNameKey(name: String?): String? =
        name?.takeIf { it.isNotBlank() }?.let { StringUtils.alphanumericKey(it) }?.takeIf { it.isNotEmpty() }
}
