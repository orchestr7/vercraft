package com.akuleshov7.vercraft

import com.akuleshov7.vercraft.core.SemVerReleaseType
import org.gradle.api.Plugin
import org.gradle.api.Project

class Extension {

}

class VercraftPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        assert(project == project.rootProject) { "Vercraft plugin should be applied to root project" }

        val extension = project.extensions.create("vercraft", VercraftExtension::class.java)

        project.tasks.register("gitVersion", GitVersionTask::class.java) {
        }

        project.tasks.register("createRelease", CreateNewVersionTask::class.java) {
            it.releaseType.set(
                project.findProperty("releaseType")
                    ?.toString()
                    ?.let { SemVerReleaseType.fromValue(it) }
                    ?: SemVerReleaseType.MINOR
            )
        }
    }
}
