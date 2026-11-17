package app.myliberty.easy_records

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes

@AnalyzeClasses(packages = ["app.myliberty.easy_records"])
class ArchitectureTest {

    // Checks classes only: SQL, such as report query builders joining tables owned by other domains,
    // is free to cross domain lines.
    @ArchTest
    val domainsShouldBeSelfContained: ArchRule = classes()
        .that(areNonServiceDomainClasses())
        .should(onlyBeAccessedFromOwnDomainOrOutsideBusiness())

    @ArchTest
    val everyDomainShouldBeInExactlyOneProductTag: ArchRule = classes()
        .that(areInsideADomain())
        .should(beInExactlyOneProductTag())

    @ArchTest
    val coreShouldNotDependOnRetail: ArchRule = classes()
        .that(areTaggedAs(ProductTag.CORE))
        .should(notDependOnDomainsTaggedAs(ProductTag.RETAIL))

    @ArchTest
    val coreShouldNotDependOnBilling: ArchRule = classes()
        .that(areTaggedAs(ProductTag.CORE))
        .should(notDependOnDomainsTaggedAs(ProductTag.BILLING))

    @ArchTest
    val retailShouldNotDependOnBilling: ArchRule = classes()
        .that(areTaggedAs(ProductTag.RETAIL))
        .should(notDependOnDomainsTaggedAs(ProductTag.BILLING))

    // Passes with no billing domains; the empty-should allowance keeps ArchUnit from failing on zero matched classes.
    @ArchTest
    val billingShouldNotDependOnRetail: ArchRule = classes()
        .that(areTaggedAs(ProductTag.BILLING))
        .should(notDependOnDomainsTaggedAs(ProductTag.RETAIL))
        .allowEmptyShould(true)

    /** A quantity is converted only by multiplying with a captured ratio inside the quantity module. */
    @ArchTest
    val noQuantityMathOnConversionRatioOutsideUtil: ArchRule = classes()
        .that(areOutsideTheQuantityAndBusinessUtilPackages())
        .should(notReadConversionRatioParts())

    /** Rounding policy lives in Decimals: its scales name the policy, a raw RoundingMode call site invents one. */
    @ArchTest
    val noRawBigDecimalRoundingOutsideUtil: ArchRule = classes()
        .that(areOutsideTheQuantityAndBusinessUtilPackages())
        .should(notRoundBigDecimalDirectly())

    private fun notRoundBigDecimalDirectly() = object : ArchCondition<JavaClass>(
        "not call BigDecimal.setScale, BigDecimal.divide or BigDecimal.round with a RoundingMode or MathContext"
    ) {
        override fun check(javaClass: JavaClass, events: ConditionEvents) {
            javaClass.methodCallsFromSelf
                .filter { call ->
                    call.targetOwner.name == BIG_DECIMAL_CLASS &&
                        call.target.name in ROUNDING_METHODS &&
                        call.target.rawParameterTypes.any { parameterType -> parameterType.name in ROUNDING_POLICY_CLASSES }
                }
                .forEach { call ->
                    events.add(SimpleConditionEvent.violated(call, "${call.description} rounds directly; use a Decimals scale"))
                }
        }
    }

    private fun areOutsideTheQuantityAndBusinessUtilPackages() = object : DescribedPredicate<JavaClass>("are outside util.quantity and util.business") {
        override fun test(javaClass: JavaClass) =
            !javaClass.packageName.startsWith("$ROOT_PACKAGE.util.quantity") &&
                !javaClass.packageName.startsWith("$ROOT_PACKAGE.util.business")
    }

    private fun notReadConversionRatioParts() = object : ArchCondition<JavaClass>(
        "not call ConversionRatio's numerator, denominator or factor"
    ) {
        override fun check(javaClass: JavaClass, events: ConditionEvents) {
            javaClass.methodCallsFromSelf
                .filter { it.targetOwner.name == CONVERSION_RATIO_CLASS && it.target.name in FORBIDDEN_RATIO_MEMBERS }
                .forEach { call ->
                    events.add(SimpleConditionEvent.violated(call, "${call.description} reads a ConversionRatio part outside the quantity module"))
                }
        }
    }

    private fun areInsideADomain() = object : DescribedPredicate<JavaClass>("are inside a domain package") {
        override fun test(javaClass: JavaClass) = ProductTags.domainOf(javaClass.packageName) != null
    }

    private fun areTaggedAs(productTag: ProductTag) = object : DescribedPredicate<JavaClass>("are tagged $productTag") {
        override fun test(javaClass: JavaClass): Boolean {
            val domain = ProductTags.domainOf(javaClass.packageName) ?: return false
            return ProductTags.tagOf(domain) == productTag
        }
    }

    private fun beInExactlyOneProductTag() = object : ArchCondition<JavaClass>(
        "be in exactly one of the core, retail or billing sets in ProductTags"
    ) {
        override fun check(javaClass: JavaClass, events: ConditionEvents) {
            val domain = ProductTags.domainOf(javaClass.packageName) ?: return
            if (ProductTags.tagsOf(domain).size != 1) {
                events.add(SimpleConditionEvent.violated(
                    javaClass,
                    "${javaClass.name} is in domain $domain, which has ${ProductTags.tagsOf(domain).size} product tags"
                ))
            }
        }
    }

    private fun notDependOnDomainsTaggedAs(forbiddenTag: ProductTag) = object : ArchCondition<JavaClass>(
        "not depend on $forbiddenTag domains"
    ) {
        override fun check(javaClass: JavaClass, events: ConditionEvents) {
            val dependentDomain = ProductTags.domainOf(javaClass.packageName) ?: return
            javaClass.directDependenciesFromSelf.forEach { dependency ->
                val dependedOnDomain = ProductTags.domainOf(dependency.targetClass.packageName)
                    ?: return@forEach
                val reachesForbiddenTag = dependedOnDomain != dependentDomain &&
                    ProductTags.tagOf(dependedOnDomain) == forbiddenTag
                val isAllowListed = (dependentDomain to dependedOnDomain) in ProductTags.allowedCoreToRetailDependencies
                if (reachesForbiddenTag && !isAllowListed) {
                    events.add(SimpleConditionEvent.violated(javaClass, dependency.description))
                }
            }
        }
    }

    private fun areNonServiceDomainClasses() = object : DescribedPredicate<JavaClass>("are non-public business domain classes") {
        override fun test(javaClass: JavaClass) =
            domainOf(javaClass.packageName) != null &&
            !javaClass.packageName.contains(".api")
    }

    private fun onlyBeAccessedFromOwnDomainOrOutsideBusiness() = object : ArchCondition<JavaClass>(
        "only be referenced from within the same domain"
    ) {
        override fun check(javaClass: JavaClass, events: ConditionEvents) {
            val classDomain = domainOf(javaClass.packageName) ?: return
            val origins = javaClass.accessesToSelf.map { it.originOwner } +
                    javaClass.directDependenciesToSelf.map { it.originClass }
            origins
                .distinct()
                .filter { origin -> domainOf(origin.packageName) != classDomain }
                .forEach { origin ->
                    events.add(SimpleConditionEvent.violated(
                        javaClass,
                        "${origin.name} (domain: ${domainOf(origin.packageName) ?: "<non-business>"}) " +
                        "references ${javaClass.name} (domain: $classDomain) — go through the .api package"
                    ))
                }
        }
    }

    private companion object {
        const val ROOT_PACKAGE = "app.myliberty.easy_records"
        const val CONVERSION_RATIO_CLASS = "$ROOT_PACKAGE.util.business.ConversionRatio"
        const val BIG_DECIMAL_CLASS = "java.math.BigDecimal"
        val ROUNDING_POLICY_CLASSES = setOf("java.math.RoundingMode", "java.math.MathContext")
        val ROUNDING_METHODS = setOf("setScale", "divide", "round")
        val FORBIDDEN_RATIO_MEMBERS = setOf(
            "getNumerator", "getDenominator", "component1", "component2", "factor"
        )
    }

    private fun domainOf(packageName: String): String? =
        ProductTags.domainOf(packageName)?.takeUnless { domain -> domain.startsWith("cross_tier.") }
}
