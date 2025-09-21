plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.20"
    application
}

group = "org.navtest"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5") // Or "kotlin-test-junit" for JUnit 4
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0") // Or "junit:junit:4.13.2" for JUnit 4
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0") // Or "org.junit.vintage:junit-vintage-engine:5.10.0" for JUnit 4
    testImplementation(kotlin("test"))
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

application {
    mainClass.set("org.navtest.MainKt") // Replace with your actual main class
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
    // Optional: Configure the 'run' task further
    jvmArgs("-Dmy.custom.setting=true")
    // If your application needs specific environment variables
    environment("APP_MODE", "development")
}