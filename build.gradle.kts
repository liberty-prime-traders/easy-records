plugins {
    id("easyrecords.spring-boot-app")
    id("easyrecords.cucumber")
    id("easyrecords.coverage")
}

group = "me.ezra-home"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.security.oauth2.resource.server)
    implementation(libs.spring.security.oauth2.jose)
    implementation(libs.google.api.client)
    implementation(libs.hibernate.envers)
    implementation(libs.spring.kafka)

    implementation(libs.jackson.module.kotlin)
    implementation(libs.gson)
    implementation(libs.kotlin.reflect)
    implementation(libs.jetbrains.annotations)

    implementation(libs.liquibase.core)
    runtimeOnly(libs.postgresql)

    implementation(libs.guava)
    implementation(libs.caffeine)
    implementation(libs.spring.boot.starter.cache)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.security.config)
    testImplementation(libs.spring.security.web)
    testImplementation(libs.h2)
    testImplementation(libs.archunit.junit5)
}
