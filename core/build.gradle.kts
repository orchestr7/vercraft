plugins {
    kotlin("jvm")
    id("maven-publish")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "core"
        }
    }

    // Publish to the local Maven repository
    repositories {
        mavenLocal()  // This will publish to ~/.m2/repository
    }
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.jgit)
    implementation(libs.slf4j.api)
    implementation(libs.log4j.slf4j2.impl)
    implementation(libs.log4j.core)
    implementation(libs.log4j.api)
}

dependencies {
    testImplementation(kotlin("test"))
}
