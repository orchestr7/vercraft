plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "1.2.1"
}

gradlePlugin {
    plugins {
        create("vercraft") {
            id = "com.akuleshov7.vercraft.plugin-gradle"
            implementationClass = "com.akuleshov7.vercraft.VercraftPlugin"
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}

dependencies {
    implementation(project(":core"))
    testImplementation(kotlin("test"))
}

