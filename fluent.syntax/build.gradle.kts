group = "org.projectfluent"
version = "0.1"

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    kotlin("jvm") version "1.3.72"

    // Apply the java-library plugin for API and implementation separation.
    `java-library`

    // Add dokka to be able to generate documentation.
    id("org.jetbrains.dokka") version "0.10.1"

    // Add linting with ktlint
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
}

repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 7 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7")

    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")

    // Use Klaxon for tests.
    testImplementation("com.beust:klaxon:5.0.1")
}

ktlint {
    version.set("0.37.2")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
