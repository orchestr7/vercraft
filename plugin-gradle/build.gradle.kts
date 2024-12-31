plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "1.2.1"
    id("com.akuleshov7.buildutils.publishing-configuration")
}

gradlePlugin {
    plugins {
        create("vercraft") {
            id = "com.akuleshov7.vercraft.plugin-gradle"
            implementationClass = "com.akuleshov7.vercraft.VercraftPlugin"
        }
    }
}

dependencies {
    implementation(project(":core"))
    testImplementation(kotlin("test"))
}

