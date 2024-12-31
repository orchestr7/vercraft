import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("io.github.gradle-nexus:publish-plugin:2.0.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0")
    implementation(kotlin("gradle-plugin"))
    implementation("org.jetbrains:annotations:26.0.1")
}
