package com.akuleshov7.vercraft

import com.akuleshov7.vercraft.core.SemVerReleaseType
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class CreateNewVersionTask : DefaultTask() {
    @get:Input
    abstract val releaseType: Property<SemVerReleaseType>

    @TaskAction
    fun createRelease() {
        com.akuleshov7.vercraft.core.createRelease(project.projectDir, releaseType.getOrElse(SemVerReleaseType.MINOR))
    }
}
