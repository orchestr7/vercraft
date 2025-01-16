package com.akuleshov7.vercraft

import com.akuleshov7.vercraft.core.Config
import com.akuleshov7.vercraft.core.DefaultConfig
import com.akuleshov7.vercraft.core.SemVerReleaseType
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import com.akuleshov7.vercraft.utils.GitUtilsTask


/**
 * Gradle task for making a new MAJOR/MINOR release (by releaseType).
 * By the Default MINOR release branch will be made.
 */
abstract class MakeReleaseTask : GitUtilsTask() {
    @get:Input
    abstract val releaseType: Property<SemVerReleaseType>

    @get:Input
    abstract val config: Property<Config>

    @TaskAction
    // TODO: unify common logic for tag and branch
    fun createRelease() {
        val version = com.akuleshov7.vercraft.core.createRelease(
            project.projectDir,
            releaseType.getOrElse(SemVerReleaseType.MINOR),
            DefaultConfig
        )

        // Push release branch
        gitPushBranch(config.get().remote, "release/$version")

        // Push release tag
        gitPushTag(config.get().remote, "v$version")
    }
}
