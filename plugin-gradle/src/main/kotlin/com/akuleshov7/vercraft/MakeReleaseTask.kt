package com.akuleshov7.vercraft

import com.akuleshov7.vercraft.core.Config
import com.akuleshov7.vercraft.core.DefaultConfig
import com.akuleshov7.vercraft.core.SemVerReleaseType
import com.akuleshov7.vercraft.core.utils.ERROR_PREFIX
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.process.ExecSpec
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import javax.inject.Inject


/**
 * Gradle task for making a new MAJOR/MINOR release (by releaseType).
 * By the Default MINOR release branch will be made.
 */
abstract class MakeReleaseTask : DefaultTask() {
    @get:Input
    abstract val releaseType: Property<SemVerReleaseType>

    @get:Input
    abstract val config: Property<Config>

    @Inject
    protected abstract fun getExecOperations(): ExecOperations?

    @TaskAction
    // TODO: unify common logic for tag and branch
    fun createRelease() {
        val output = ByteArrayOutputStream()
        val error = ByteArrayOutputStream()
        val version = com.akuleshov7.vercraft.core.createRelease(
            project.projectDir,
            releaseType.getOrElse(SemVerReleaseType.MINOR),
            DefaultConfig
        )

        // === push release branch to remote
        var result = getExecOperations()!!.exec { spec: ExecSpec ->
            spec.workingDir = project.projectDir
            // TODO: make remote configurable (now origin)
            spec.setCommandLine("git", "push", "-u", "origin", "release/$version")
            spec.setStandardOutput(output)
            spec.setErrorOutput(error)
            spec.setIgnoreExitValue(true)
        }

        if(result.exitValue != 0) {
            val errorStr = error.toString(StandardCharsets.UTF_8)
            logger.info("$ERROR_PREFIX $errorStr")
            val extraInfo = when {
                errorStr.contains("fatal: unable to access") -> {
                    "Please check your connection to git hosting."
                }
                else -> ""
            }
            throw GradleException("$ERROR_PREFIX Unable to push release branch to remote. $extraInfo")
        }

        // === push release tag to remote
        result = getExecOperations()!!.exec { spec: ExecSpec ->
            spec.workingDir = project.projectDir
            // TODO: make remote configurable (now origin)
            spec.setCommandLine("git", "push", "origin", "tag", "v$version")
            spec.setStandardOutput(output)
            spec.setErrorOutput(error)
            spec.setIgnoreExitValue(true)
        }

        if(result.exitValue != 0) {
            val errorStr = error.toString(StandardCharsets.UTF_8)
            logger.info("$ERROR_PREFIX $errorStr")
            val extraInfo = when {
                errorStr.contains("fatal: unable to access") -> {
                    "Please check your connection to git hosting."
                }
                else -> ""
            }
            throw GradleException("$ERROR_PREFIX Unable to push release tag to remote. $extraInfo")
        }
    }
}
