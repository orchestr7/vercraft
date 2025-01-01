import com.akuleshov7.buildutils.configurePom

plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    // id("com.gradle.plugin-publish") version "1.2.1"
    id("com.akuleshov7.buildutils.publishing-default-configuration")
}

gradlePlugin {
    plugins {
        create("vercraft") {
            id = "com.akuleshov7.vercraft.plugin-gradle"
            implementationClass = "com.akuleshov7.vercraft.VercraftPlugin"
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            withType<MavenPublication> {
                pom {
                    configurePom(project)
                }
            }
        }
    }
}

dependencies {
    implementation(project(":core"))
    testImplementation(kotlin("test"))
}

