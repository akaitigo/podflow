plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.allopen") version "2.1.0"
    id("io.quarkus") version "3.34.2"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

repositories {
    mavenCentral()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-grpc")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.quarkus:quarkus-jdbc-h2")
    testImplementation("io.quarkus:quarkus-test-h2")
}

group = "com.akaitigo"
version = "0.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_21.toString()
    kotlinOptions.javaParameters = true
}

allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.persistence.Entity")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

detekt {
    config.setFrom("$projectDir/detekt.yml")
    parallel = true
    buildUponDefaultConfig = true
}

// Sync proto definitions from the shared proto/ directory to backend/src/main/proto/.
// This ensures proto/ is the single source of truth (M-1).
val syncProto by tasks.registering(Sync::class) {
    from("${rootProject.projectDir}/../proto/podflow")
    into("$projectDir/src/main/proto/podflow")
}

tasks.named("processResources") {
    dependsOn(syncProto)
}

// Remove stale Quarkus-generated gRPC sources from src/main/java/ during clean.
// Quarkus code generation writes output to build/, but older runs or version
// mismatches can leave protobuf-lite artefacts under src/main/java/ that shadow
// the correct full-protobuf classes in build/. Deleting them prevents
// NoSuchMethodError at test time (GeneratedMessageLite vs GeneratedMessageV3).
tasks.named<Delete>("clean") {
    delete("$projectDir/src/main/java")
}
