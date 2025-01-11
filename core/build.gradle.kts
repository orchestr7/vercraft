plugins {
    kotlin("jvm")
    id("com.akuleshov7.buildutils.publishing-default-configuration")
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
