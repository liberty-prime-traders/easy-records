plugins {
    id("easyrecords.kotlin-library")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    kotlin("kapt")
    id("io.spring.dependency-management")
}

val catalog = the<VersionCatalogsExtension>().named("libs")

extra["testcontainers.version"] = catalog.findVersion("testcontainers").get().requiredVersion
extra["junit-jupiter.version"] = catalog.findVersion("junit-jupiter").get().requiredVersion

// Importing the BOM here lets a module use Spring-managed versions without applying the Boot plugin.
dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${catalog.findVersion("spring-boot").get()}")
    }
}

dependencies {
    implementation(catalog.findLibrary("mapstruct").get())
    kapt(catalog.findLibrary("mapstruct-processor").get())
}

kapt {
    correctErrorTypes = true
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.matching { it.name == "kaptTestKotlin" }.configureEach {
    enabled = false
}
