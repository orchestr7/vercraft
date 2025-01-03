package com.akuleshov7.vercraft

import org.gradle.api.Plugin
import org.gradle.api.Project

const val RELEASE_TYPE = "releaseType"

class VercraftPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        assert(project == project.rootProject) { "Vercraft plugin should be applied to root project" }

        val extension = project.extensions.create("vercraft", VercraftExtension::class.java)
        extension.setReleaseTypeFromProps(
            project.findProperty(RELEASE_TYPE)
        )

        // TODO: think also about changing the version in settings.gradle
        gitVersion(project)
        makeRelease(project, extension)
    }

    private fun gitVersion(project: Project) =
        project.tasks.register("gitVersion", GitVersionTask::class.java) { }

    private fun makeRelease(project: Project, extension: VercraftExtension) =
        project.tasks.register("makeRelease", MakeReleaseTask::class.java) {
            it.releaseType.set(extension.releaseType)
        }
}
