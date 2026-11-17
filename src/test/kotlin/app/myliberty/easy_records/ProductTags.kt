package app.myliberty.easy_records

enum class ProductTag {
    CORE,
    RETAIL,
    BILLING
}

/**
 * Product classification of every domain package, written relative to the root package. A domain under a
 * `retail` or `billing` folder (`<tier>.business.retail.<domain>`, `cross_tier.retail.<domain>`) takes that
 * product from its path; every other domain is core and must be listed in [coreDomains]. `configuration`,
 * `messaging`, `util` and the `rest` packages are infrastructure, not domains, and carry no tag.
 * `ArchitectureTest` fails on a domain that is neither under a product folder nor in [coreDomains].
 */
object ProductTags {

    private const val ROOT_PACKAGE = "app.myliberty.easy_records"
    private val TIER_PACKAGES = setOf("platform", "organizations", "locations")
    private val PRODUCT_FOLDER_TAGS = mapOf("retail" to ProductTag.RETAIL, "billing" to ProductTag.BILLING)

    val coreDomains: Set<String> = setOf(
        "platform.business.authority",
        "platform.business.authorization_pass",
        "platform.business.db_migration",
        "platform.business.db_version",
        "platform.business.feature",
        "platform.business.identity",
        "platform.business.jurisdiction",
        "platform.business.jurisdiction_tax_type",
        "platform.business.jurisdiction_type",
        "platform.business.organization",
        "platform.business.organization_join_request",
        "platform.business.permission_assignment",
        "platform.business.reserved_subdomain",
        "platform.business.revision",
        "platform.business.role_assignment",
        "platform.business.startup_checks",
        "platform.business.sysuser",
        "platform.business.table_registry",
        "platform.business.tax_type",
        "cross_tier.authority",
        "cross_tier.expense",
        "organizations.business.account",
        "organizations.business.account_selection",
        "organizations.business.accounting_config",
        "organizations.business.address",
        "organizations.business.contact",
        "organizations.business.contact_opening_balance",
        "organizations.business.expense_type",
        "organizations.business.feature",
        "organizations.business.fiscal_period",
        "organizations.business.fund_movement",
        "organizations.business.jobtitle",
        "organizations.business.kafka_log",
        "organizations.business.ledger",
        "organizations.business.liberty_access",
        "organizations.business.loan",
        "organizations.business.location",
        "organizations.business.lock",
        "organizations.business.manual_journal",
        "organizations.business.opening_balance",
        "organizations.business.org_expense",
        "organizations.business.org_jurisdiction_tax_type",
        "organizations.business.org_profile",
        "organizations.business.org_table_registry",
        "organizations.business.organization_admin",
        "organizations.business.organization_user",
        "organizations.business.payment_method",
        "organizations.business.permission_assignment",
        "organizations.business.revision",
        "organizations.business.role_assignment",
        "organizations.business.tag",
        "organizations.business.tax_rate",
        "locations.business.expense",
        "locations.business.location_user",
        "locations.business.lock",
        "locations.business.permission_assignment",
        "locations.business.revision",
        "locations.business.role_assignment"
    )

    /**
     * Dependencies from a core domain into a retail domain, as (dependent domain, depended-on domain).
     * Ledger posts accounting entries for retail documents and the expense domains read sale, purchase and
     * stock-transfer data, so they reach into retail api packages. Entries leave this set when the dependency does.
     */
    val allowedCoreToRetailDependencies: Set<Pair<String, String>> = setOf(
        "locations.business.expense" to "locations.business.retail.purchase",
        "locations.business.expense" to "locations.business.retail.sale",
        "organizations.business.ledger" to "locations.business.retail.stock_revision",
        "organizations.business.ledger" to "locations.business.retail.tax_entry",
        "organizations.business.ledger" to "organizations.business.retail.stock_transfer",
        "organizations.business.org_expense" to "organizations.business.retail.stock_transfer"
    )

    fun domainOf(packageName: String): String? {
        if (!packageName.startsWith("$ROOT_PACKAGE.")) return null
        val packageSegments = packageName.removePrefix("$ROOT_PACKAGE.").split(".")
        val tierOrCrossTier = packageSegments.first()
        val domainDepth = when {
            tierOrCrossTier == "cross_tier" -> 2
            tierOrCrossTier in TIER_PACKAGES && packageSegments.getOrNull(1) == "business" -> 3
            else -> return null
        }
        val productFolderDepth = domainDepth - 1
        val isUnderProductFolder = packageSegments.getOrNull(productFolderDepth) in PRODUCT_FOLDER_TAGS
        val segmentCount = if (isUnderProductFolder) domainDepth + 1 else domainDepth
        return if (packageSegments.size >= segmentCount) packageSegments.take(segmentCount).joinToString(".") else null
    }

    fun tagsOf(domain: String): Set<ProductTag> {
        val domainSegments = domain.split(".")
        val productFolderDepth = if (domainSegments.first() == "cross_tier") 1 else 2
        val productFolderTag = PRODUCT_FOLDER_TAGS[domainSegments.getOrNull(productFolderDepth)]
        return buildSet {
            if (domain in coreDomains) add(ProductTag.CORE)
            if (productFolderTag != null) add(productFolderTag)
        }
    }

    fun tagOf(domain: String): ProductTag? = tagsOf(domain).singleOrNull()
}
