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
}

dependencies {
    testImplementation(kotlin("test"))
}
