package com.akuleshov7.vercraft

import com.akuleshov7.vercraft.core.*
import com.akuleshov7.vercraft.utils.runGitCommand
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.process.ExecOperations
import javax.inject.Inject

const val DEFAULT_MAIN_BRANCH = "defaultMainBranch"
const val CHECKOUT_BRANCH = "checkOutBranch"
const val RELEASE_TYPE = "releaseType"
const val SEM_VER = "semVer"
const val REMOTE = "remote"

class VercraftPlugin @Inject constructor(
    // Injected ExecOperations
    private val execOperations: ExecOperations
) : Plugin<Project> {
    override fun apply(project: Project) {
        assert(project == project.rootProject) { "Vercraft plugin should be applied to root project" }

        val extension = project.extensions.create("vercraft", VercraftExtension::class.java)

        // === getting gradle properties for the configuration of VerCraft
        extension.setReleaseTypeFromProps(
            project.findProperty(RELEASE_TYPE)
        )
        extension.setSemVerFromProps(
            project.findProperty(SEM_VER)
        )

        val config = Config(
            DefaultMainBranch(project.findProperty(DEFAULT_MAIN_BRANCH)?.toString() ?: DefaultConfig.defaultMainBranch.value),
            Remote(project.findProperty(REMOTE)?.toString() ?: DefaultConfig.remote.value),
            project.findProperty(CHECKOUT_BRANCH)?.toString()?.let { CheckoutBranch(it) }
        )
        extension.config.set(config)

        // === fetching project
        runGitCommand(
            listOf("git", "fetch", config.remote.value, "--prune", "--tags"),
            "Unable to fetch project from the remote.",
            execOperations,
            project,
            project.logger,
            failOnError = false
        )

        // TODO: think also about changing the version in settings.gradle
        val ver = gitVersion(project.projectDir, config)
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
            it.config.set(extension.config)
            it.semVer.set(extension.semVer)
        }
}
