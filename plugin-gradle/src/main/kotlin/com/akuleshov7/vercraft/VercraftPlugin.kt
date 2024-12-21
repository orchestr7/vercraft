package com.akuleshov7.vercraft

import com.akuleshov7.vercraft.core.SemVerReleaseType
import com.akuleshov7.vercraft.core.createRelease
import com.akuleshov7.vercraft.core.getVersion
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class GitVersionTask : DefaultTask() {
    @TaskAction
    fun action() {
        getVersion(project.projectDir)
    }
}

abstract class CreateNewVersion : DefaultTask() {

    @get:Input
    abstract val releaseType: Property<SemVerReleaseType>

    @TaskAction
    fun action() {
        createRelease(project.projectDir, releaseType.getOrElse(SemVerReleaseType.MINOR))
    }
}

class VercraftPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("gitVersion", GitVersionTask::class.java) {
        }

        project.tasks.register("createRelease", CreateNewVersion::class.java) {
            it.releaseType.set(
                project.findProperty("releaseType")
                    ?.toString()
                    ?.let { SemVerReleaseType.fromValue(it) }
                    ?: SemVerReleaseType.MINOR
            )
        }
    }
}
