plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.serialization") version "1.9.25"
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.owomeb.backend"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/releases/") }
    maven("https://jcenter.bintray.com/")
}


dependencies {

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.security:spring-security-config:6.1.2")
    implementation("org.springframework.security:spring-security-web:6.1.2")
    implementation("org.springframework.security:spring-security-core:6.1.2")
    implementation("org.seleniumhq.selenium:selenium-java:4.20.0")
    implementation("io.github.bonigarcia:webdrivermanager:5.8.0")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("org.freehep:freehep-graphicsio-emf:2.4")
    implementation("com.google.guava:guava:32.1.2-jre")


    implementation("org.apache.poi:poi-scratchpad:5.2.5")
    implementation("commons-io:commons-io:2.15.1")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    runtimeOnly("org.postgresql:postgresql:42.7.5")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")


    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.json:json:20240303")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")

    // JSON i serializacja
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Apache POI (obsługa .doc i .docx)
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")

    // Kompresja ZIP
    implementation("org.apache.commons:commons-compress:1.21")
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Testy
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
    sourceSets.all {
        languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
