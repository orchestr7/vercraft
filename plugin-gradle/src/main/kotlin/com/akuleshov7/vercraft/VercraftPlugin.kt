package com.akuleshov7.vercraft

import com.akuleshov7.vercraft.core.Config
import com.akuleshov7.vercraft.core.DefaultConfig
import com.akuleshov7.vercraft.core.getVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

const val DEFAULT_MAIN_BRANCH = "defaultMainBranch"
const val CHECKOUT_BRANCH = "checkOutBranch"
const val RELEASE_TYPE = "releaseType"
const val REMOTE = "remote"

class VercraftPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // TODO: need to fetch the project first

        assert(project == project.rootProject) { "Vercraft plugin should be applied to root project" }

        val extension = project.extensions.create("vercraft", VercraftExtension::class.java)
        extension.setReleaseTypeFromProps(
            project.findProperty(RELEASE_TYPE)
        )
        extension.config.set(
            Config(
                project.findProperty(DEFAULT_MAIN_BRANCH)?.toString() ?: DefaultConfig.defaultMainBranch,
                project.findProperty(REMOTE)?.toString() ?: DefaultConfig.remote,
                project.findProperty(CHECKOUT_BRANCH)?.toString() ?: DefaultConfig.checkoutBranch
            )
        )

        // TODO: think also about changing the version in settings.gradle
        val ver = getVersion(project.projectDir, DefaultConfig)
        project.allprojects.forEach {
            it.version = ver
        }

        // registering tasks 'gitVersion' and 'makeRelease' for any manual run
        gitVersion(project, extension)
        makeRelease(project, extension)
    }

    private fun gitVersion(project: Project, extension: VercraftExtension) =
        project.tasks.register("gitVersion", GitVersionTask::class.java) {
            it.config.set(extension.config)
        }

    private fun makeRelease(project: Project, extension: VercraftExtension) =
        project.tasks.register("makeRelease", MakeReleaseTask::class.java) {
            it.releaseType.set(extension.releaseType)
        }
}
