import cz.habarta.typescript.generator.JsonLibrary
import cz.habarta.typescript.generator.TypeScriptFileType
import cz.habarta.typescript.generator.TypeScriptOutputKind

plugins {
    id("java")
    id("application")
    id("cz.habarta.typescript-generator") version "3.2.1263"
}

group = "com.starlingbank"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val jerseyVersion = "3.1.5"

dependencies {
    // Jersey JAX-RS with embedded Grizzly HTTP server
    implementation("org.glassfish.jersey.core:jersey-server:$jerseyVersion")
    implementation("org.glassfish.jersey.containers:jersey-container-grizzly2-http:$jerseyVersion")
    implementation("org.glassfish.jersey.inject:jersey-hk2:$jerseyVersion")
    implementation("org.glassfish.jersey.media:jersey-media-json-jackson:$jerseyVersion")

    // Guice dependency injection
    implementation("com.google.inject:guice:7.0.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.starlingbank.Main")
}

tasks.test {
    useJUnitPlatform()
}

tasks.generateTypeScript {
    jsonLibrary = JsonLibrary.jackson2
    outputKind = TypeScriptOutputKind.module
    outputFileType = TypeScriptFileType.implementationFile
    classPatterns = mutableListOf("com.starlingbank.model.**")
    classes = mutableListOf(
        "com.starlingbank.HelloResource",
        "com.starlingbank.api.DeskResource",
        "com.starlingbank.api.EmployeeResource",
        "com.starlingbank.api.BookingResource",
        "com.starlingbank.api.AssignmentResource"
    )
    outputFile = "frontend/src/generated/api.ts"
    generateJaxrsApplicationClient = true
    dependsOn(tasks.compileJava)
}