import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    java
    jacoco
    id("info.solidsoft.pitest")
}

val catalog = the<VersionCatalogsExtension>().named("libs")

jacoco {
    toolVersion = catalog.findVersion("jacoco").get().requiredVersion
}

// -PcoveragePackage=app.myliberty.easy_records.organizations.business.core.account narrows instrumentation, the unit
// test selection, the reports and PIT to that package and its subpackages. Without it the whole codebase is covered.
val coveragePackage: String? = (findProperty("coveragePackage") as String?)?.takeIf { it.isNotBlank() }
val coveragePackagePath: String? = coveragePackage?.replace('.', '/')

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        if (coveragePackage != null) {
            includes = listOf("$coveragePackage.*")
        }
    }
}

tasks.named<Test>("test") {
    if (coveragePackage != null) {
        filter {
            includeTestsMatching("$coveragePackage.*")
            isFailOnNoMatchingTests = false
        }
    }
}

fun registerCoverageReportTask(taskName: String, executionTasks: List<String>, reportFolder: String) {
    tasks.register<JacocoReport>(taskName) {
        group = "verification"
        description = "Coverage report for $reportFolder, narrowed by -PcoveragePackage when given."
        // mustRunAfter orders the report behind a Test task requested in the same invocation without forcing it to run.
        // The exec path is read from the jacoco default, not mapped from the task: a provider derived from the task
        // would make every report run its Test task first, without the sweep's -Dcucumber.features.
        executionTasks.forEach { executionTaskName ->
            mustRunAfter(executionTaskName)
            executionData.from(layout.buildDirectory.file("jacoco/$executionTaskName.exec"))
        }
        classDirectories.setFrom(
            sourceSets.main.get().output.classesDirs.asFileTree.matching {
                if (coveragePackagePath != null) include("$coveragePackagePath/**")
            }
        )
        sourceDirectories.setFrom(sourceSets.main.get().allSource.srcDirs)
        reports {
            xml.required = true
            html.required = true
            xml.outputLocation = layout.buildDirectory.file("reports/coverage/$reportFolder/jacoco.xml")
            html.outputLocation = layout.buildDirectory.dir("reports/coverage/$reportFolder/html")
        }
        onlyIf { executionData.files.any { it.exists() } }
    }
}

registerCoverageReportTask("unitCoverageReport", listOf("test"), "unit")

// The cucumber-backed reports need the cucumberCoverageTest task, so they wait for that plugin whichever applies first.
pluginManager.withPlugin("easyrecords.cucumber") {
    registerCoverageReportTask("integrationCoverageReport", listOf("cucumberCoverageTest"), "integration")
    registerCoverageReportTask("mergedCoverageReport", listOf("test", "cucumberCoverageTest"), "merged")
}

// Mutation testing runs the unit tests only: cucumber needs containers and is too slow to repeat per mutant.
// -PcoveragePackage is required so a run stays minutes, not hours; the pitest task fails fast without it.
val pitestScope = "${coveragePackage ?: "app.myliberty.easy_records"}.*"

pitest {
    pitestVersion = catalog.findVersion("pitest").get().requiredVersion
    junit5PluginVersion = catalog.findVersion("pitest-junit5-plugin").get().requiredVersion
    targetClasses = setOf(pitestScope)
    targetTests = setOf(pitestScope)
    excludedClasses = setOf("*MapperImpl", "*Application*")
    // Kotlin's generated null checks mutate into survivors that no test can or should kill.
    avoidCallsTo = setOf("kotlin.jvm.internal.Intrinsics")
    threads = Runtime.getRuntime().availableProcessors()
    outputFormats = setOf("XML", "HTML")
    timestampedReports = false
    mutators = setOf("DEFAULTS")
}

tasks.named("pitest") {
    doFirst {
        check(coveragePackage != null) {
            "Pass -PcoveragePackage=<dotted.package>; a whole-codebase mutation run is too slow."
        }
    }
}
