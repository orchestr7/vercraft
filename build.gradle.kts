project.description = "Versioning tool for real enterprise"

plugins {
    alias(libs.plugins.kotlin.jvm)
    java
    id("com.akuleshov7.buildutils.publishing-configuration")
    id("com.akuleshov7.vercraft.plugin-gradle") version("0.6.0")
}
