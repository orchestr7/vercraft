package com.akuleshov7.vercraft

import com.akuleshov7.vercraft.core.SemVerReleaseType
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Gradle task for making a new MAJOR/MINOR release (by releaseType).
 * By the Default MINOR release branch will be made.
 */
abstract class MakeReleaseTask : DefaultTask() {
    @get:Input
    abstract val releaseType: Property<SemVerReleaseType>

    @TaskAction
    fun createRelease() {
        val version = com.akuleshov7.vercraft.core.createRelease(project.projectDir, releaseType.getOrElse(SemVerReleaseType.MINOR))
        logger.warn("Successfully \"VerCrafted\" the release [$version]")
    }
}
