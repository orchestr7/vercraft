project.description = "Versioning tool for real enterprise"

plugins {
    kotlin("jvm") version ("2.1.0")
    java
    id("com.akuleshov7.buildutils.publishing-configuration")
    // id("com.akuleshov7.vercraft.plugin-gradle") version("0.0.2")
}

println("=== ${System.getenv("GITHUB_HEAD_REF")}")
println("=== ${System.getenv("GITHUB_REF_NAME")}")
println("=== ${System.getenv("GITHUB_REF")}")
