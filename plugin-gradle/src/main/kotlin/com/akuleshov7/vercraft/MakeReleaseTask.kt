package com.akuleshov7.vercraft

import com.akuleshov7.vercraft.core.Config
import com.akuleshov7.vercraft.core.DefaultConfig
import com.akuleshov7.vercraft.core.SemVer
import com.akuleshov7.vercraft.core.SemVerReleaseType
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import com.akuleshov7.vercraft.utils.GitUtilsTask
import org.gradle.api.tasks.Optional


/**
 * Gradle task for making a new MAJOR/MINOR release (by releaseType).
 * By the Default MINOR release branch will be made.
 */
abstract class MakeReleaseTask : GitUtilsTask() {
    @get:Input
    abstract val releaseType: Property<SemVerReleaseType>

    @get:Input
    @get:Optional
    abstract val semVer: Property<SemVer>

    @get:Input
    abstract val config: Property<Config>

    @TaskAction
    fun createRelease() {
        val semVerVal = semVer.orNull

        val version = if (semVerVal != null) {
            com.akuleshov7.vercraft.core.createRelease(
                project.projectDir,
                semVerVal,
                DefaultConfig
            )
        } else {
            com.akuleshov7.vercraft.core.createRelease(
                project.projectDir,
                releaseType.getOrElse(SemVerReleaseType.MINOR),
                DefaultConfig
            )
        }

        // Push release branch
        gitPushBranch(config.get().remote, "release/$version")

        // Push release tag
        gitPushTag(config.get().remote, "v$version")
    }
}
