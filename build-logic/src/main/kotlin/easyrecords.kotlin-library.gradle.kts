plugins {
    kotlin("jvm")
}

val catalog = the<VersionCatalogsExtension>().named("libs")

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

dependencies {
    testImplementation(catalog.findLibrary("kotlin-test-junit5").get())
}

tasks.withType<Test> {
    useJUnitPlatform()
}
