package app.myliberty.easy_records

enum class ProductTag {
    CORE,
    RETAIL,
    BILLING
}

/**
 * Product classification of every domain package, written relative to the root package. A domain takes its product
 * from the folder it sits in: `<tier>.business.<product>.<domain>` or `cross_tier.<product>.<domain>`, where the
 * product folder is `core`, `retail` or `billing`. The `platform` tier holds only core domains and has no product
 * folders. Endpoints are classified the same way by `<tier>.rest.endpoints.<product>`; they are not domains, so the
 * boundary rule ignores them. `configuration`, `messaging` and `util` are infrastructure and carry no tag.
 * `ArchitectureTest` fails on a domain or endpoint package that is not under a product folder.
 */
object ProductTags {

    private const val ROOT_PACKAGE = "app.myliberty.easy_records"
    private const val PLATFORM_TIER = "platform"
    private val TIER_PACKAGES = setOf(PLATFORM_TIER, "organizations", "locations")
    private val PRODUCT_FOLDER_TAGS = mapOf(
        "core" to ProductTag.CORE,
        "retail" to ProductTag.RETAIL,
        "billing" to ProductTag.BILLING
    )

    /**
     * Dependencies from a core domain into a retail domain, as (dependent domain, depended-on domain).
     * Ledger posts accounting entries for retail documents and the expense domains read sale, purchase and
     * stock-transfer data, so they reach into retail api packages. Entries leave this set when the dependency does.
     */
    val allowedCoreToRetailDependencies: Set<Pair<String, String>> = setOf(
        "locations.business.core.expense" to "locations.business.retail.purchase",
        "locations.business.core.expense" to "locations.business.retail.sale",
        "organizations.business.core.ledger" to "locations.business.retail.stock_revision",
        "organizations.business.core.ledger" to "organizations.business.retail.stock_transfer",
        "organizations.business.core.org_expense" to "organizations.business.retail.stock_transfer"
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
        val isUnderProductFolder = tierOrCrossTier != PLATFORM_TIER &&
            packageSegments.getOrNull(domainDepth - 1) in PRODUCT_FOLDER_TAGS
        val segmentCount = if (isUnderProductFolder) domainDepth + 1 else domainDepth
        return if (packageSegments.size >= segmentCount) packageSegments.take(segmentCount).joinToString(".") else null
    }

    /** A domain, or the endpoint package of a tier, as the unit that carries a product tag. */
    fun productScopeOf(packageName: String): String? {
        domainOf(packageName)?.let { domain -> return domain }
        if (!packageName.startsWith("$ROOT_PACKAGE.")) return null
        val packageSegments = packageName.removePrefix("$ROOT_PACKAGE.").split(".")
        val isEndpointPackage = packageSegments.first() in TIER_PACKAGES &&
            packageSegments.getOrNull(1) == "rest" &&
            packageSegments.getOrNull(2) == "endpoints"
        if (!isEndpointPackage) return null
        val hasProductFolder = packageSegments.first() != PLATFORM_TIER &&
            packageSegments.getOrNull(3) in PRODUCT_FOLDER_TAGS
        val segmentCount = if (hasProductFolder) 4 else 3
        return packageSegments.take(segmentCount).joinToString(".")
    }

    fun tagsOf(scope: String): Set<ProductTag> {
        val scopeSegments = scope.split(".")
        if (scopeSegments.first() == PLATFORM_TIER) return setOf(ProductTag.CORE)
        val productFolderDepth = when {
            scopeSegments.first() == "cross_tier" -> 1
            scopeSegments.getOrNull(1) == "rest" -> 3
            else -> 2
        }
        val productFolderTag = PRODUCT_FOLDER_TAGS[scopeSegments.getOrNull(productFolderDepth)]
        return setOfNotNull(productFolderTag)
    }

    fun tagOf(scope: String): ProductTag? = tagsOf(scope).singleOrNull()
}
