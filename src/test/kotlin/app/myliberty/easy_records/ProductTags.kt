package app.myliberty.easy_records

enum class ProductTag {
    CORE,
    RETAIL,
    BILLING
}

/**
 * Product classification of every domain package: `<tier>.business.<domain>` and `cross_tier.<domain>`,
 * written relative to the root package. `configuration`, `messaging`, `util` and the `rest` packages are
 * infrastructure, not domains, and carry no tag. `ArchitectureTest` fails on a domain missing from these sets.
 */
object ProductTags {

    private const val ROOT_PACKAGE = "app.myliberty.easy_records"
    private val TIER_PACKAGES = setOf("platform", "organizations", "locations")

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

    // adjustment_reason is tagged retail because its only dependents are sale, sale_adjustment, sale_session and
    // product_sales_report.
    val retailDomains: Set<String> = setOf(
        "cross_tier.product",
        "organizations.business.adjustment_reason",
        "organizations.business.daily_business_summary",
        "organizations.business.product",
        "organizations.business.product_bulk_import",
        "organizations.business.product_category",
        "organizations.business.product_group",
        "organizations.business.product_tag",
        "organizations.business.stock_item_source",
        "organizations.business.stock_movement_reason",
        "organizations.business.stock_transfer",
        "organizations.business.unit_bulk_import",
        "organizations.business.unitconversion",
        "organizations.business.unitgroup",
        "organizations.business.unitvalue",
        "locations.business.catalog_sync",
        "locations.business.delivery",
        "locations.business.location_product",
        "locations.business.opening_stock",
        "locations.business.product_purchases_report",
        "locations.business.product_sales_report",
        "locations.business.purchase",
        "locations.business.purchase_movement_context_backfill",
        "locations.business.reports",
        "locations.business.sale",
        "locations.business.sale_adjustment",
        "locations.business.sale_cogs_backfill",
        "locations.business.sale_payment",
        "locations.business.sale_session",
        "locations.business.stock",
        "locations.business.stock_adjustment",
        "locations.business.stock_adjustment_backfill",
        "locations.business.stock_reservation",
        "locations.business.stock_revision",
        "locations.business.stock_transfer",
        "locations.business.supplier_payment",
        "locations.business.supplier_return",
        "locations.business.tax_entry"
    )

    val billingDomains: Set<String> = emptySet()

    /**
     * Dependencies from a core domain into a retail domain, as (dependent domain, depended-on domain).
     * Ledger posts accounting entries for retail documents and the expense domains read sale, purchase and
     * stock-transfer data, so they reach into retail api packages. Entries leave this set when the dependency does.
     */
    val allowedCoreToRetailDependencies: Set<Pair<String, String>> = setOf(
        "locations.business.expense" to "locations.business.purchase",
        "locations.business.expense" to "locations.business.sale",
        "organizations.business.ledger" to "locations.business.stock_revision",
        "organizations.business.ledger" to "locations.business.tax_entry",
        "organizations.business.ledger" to "organizations.business.stock_transfer",
        "organizations.business.org_expense" to "organizations.business.stock_transfer"
    )

    fun domainOf(packageName: String): String? {
        if (!packageName.startsWith("$ROOT_PACKAGE.")) return null
        val packageSegments = packageName.removePrefix("$ROOT_PACKAGE.").split(".")
        val tierOrCrossTier = packageSegments.first()
        if (tierOrCrossTier == "cross_tier" && packageSegments.size >= 2) {
            return packageSegments.take(2).joinToString(".")
        }
        val isBusinessDomain = tierOrCrossTier in TIER_PACKAGES && packageSegments.getOrNull(1) == "business" &&
            packageSegments.size >= 3
        return if (isBusinessDomain) packageSegments.take(3).joinToString(".") else null
    }

    fun tagsOf(domain: String): Set<ProductTag> = buildSet {
        if (domain in coreDomains) add(ProductTag.CORE)
        if (domain in retailDomains) add(ProductTag.RETAIL)
        if (domain in billingDomains) add(ProductTag.BILLING)
    }

    fun tagOf(domain: String): ProductTag? = tagsOf(domain).singleOrNull()
}
