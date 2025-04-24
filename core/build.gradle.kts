plugins {
    kotlin("jvm")
    id("com.akuleshov7.buildutils.publishing-default-configuration")
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
