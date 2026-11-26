plugins {
    id("easyrecords.kotlin-library")
}

val catalog = the<VersionCatalogsExtension>().named("libs")

val cucumberTestSourceSet: SourceSet = sourceSets.create("cucumberTest") {
    compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
}

configurations.named("cucumberTestImplementation") {
    extendsFrom(configurations.testImplementation.get())
}
configurations.named("cucumberTestRuntimeOnly") {
    extendsFrom(configurations.testRuntimeOnly.get())
}

val cucumberRuntimeOnlyLibraries = listOf("junit-platform-suite-engine", "junit-platform-launcher")
val cucumberImplementationLibraries = listOf(
    "cucumber-java",
    "cucumber-spring",
    "cucumber-junit-platform-engine",
    "rest-assured",
    "rest-assured-kotlin-extensions",
    "junit-platform-suite-api",
    "testcontainers-postgresql",
    "testcontainers-kafka",
    "awaitility-kotlin"
)

dependencies {
    cucumberRuntimeOnlyLibraries.forEach { libraryAlias ->
        add("cucumberTestRuntimeOnly", catalog.findLibrary(libraryAlias).get())
    }
    cucumberImplementationLibraries.forEach { libraryAlias ->
        add("cucumberTestImplementation", catalog.findLibrary(libraryAlias).get())
    }
}

// The default applies to every Test task; a lane that sets its own tag expression keeps it.
tasks.withType<Test>().configureEach {
    systemProperty("cucumber.junit-platform.naming-strategy", "long")
    if (!systemProperties.containsKey("cucumber.filter.tags")) {
        systemProperty("cucumber.filter.tags", System.getProperty("cucumber.filter.tags") ?: "not @ignore")
    }
}

fun registerCucumberLaneTask(
    taskName: String,
    tagExpression: String,
    descriptionText: String,
    configureLane: Test.() -> Unit = {}
) {
    tasks.register<Test>(taskName) {
        group = "verification"
        description = descriptionText
        testClassesDirs = cucumberTestSourceSet.output.classesDirs
        classpath = cucumberTestSourceSet.runtimeClasspath
        systemProperty("cucumber.filter.tags", tagExpression)
        configureLane()
    }
}

registerCucumberLaneTask(
    "cucumberSmokeTest",
    "@smoke and not @ignore",
    "Runs only cucumber smoke scenarios."
)

registerCucumberLaneTask(
    "cucumberKafkaProducerTest",
    "@publishes-to-kafka and not @consumes-from-kafka and not @ignore",
    "Runs cucumber kafka producer scenarios."
)

registerCucumberLaneTask(
    "cucumberKafkaConsumerTest",
    "@consumes-from-kafka and not @ignore",
    "Runs cucumber kafka consumer scenarios."
)

registerCucumberLaneTask(
    "cucumberRegressionTest",
    "not @ignore",
    "Runs the full cucumber regression suite."
)

// A separate task from cucumberRegressionTest so coverage runs never overwrite regression results, and so the
// coverage data lands in its own build/jacoco/cucumberCoverageTest.exec.
registerCucumberLaneTask(
    "cucumberCoverageTest",
    System.getProperty("cucumber.filter.tags") ?: "not @ignore",
    "Runs the cucumber suite for coverage; honors -Dcucumber.filter.tags, -Dcucumber.features and -PcoveragePackage."
) {
    val selectedFeaturePaths: String? = System.getProperty("cucumber.features")
    if (selectedFeaturePaths != null) {
        systemProperty("cucumber.features", selectedFeaturePaths)
        // The Postgres query tests also live in the cucumberTest source set; they would run on every feature sweep.
        useJUnitPlatform { includeEngines("junit-platform-suite") }
    }
}
