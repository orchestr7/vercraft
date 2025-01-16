package com.akuleshov7.vercraft.utils

import com.akuleshov7.vercraft.core.utils.ERROR_PREFIX
import com.akuleshov7.vercraft.core.utils.WARN_PREFIX
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.process.ExecOperations
import org.gradle.process.ExecSpec
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import javax.inject.Inject

public abstract class GitUtilsTask : DefaultTask() {
    @Inject
    protected abstract fun getExecOperations(): ExecOperations?

    fun gitPushBranch(remote: String, branch: String) {
        runGitCommand(
            listOf("git", "push", "-u", remote, branch),
            "Unable to push release branch to remote.",
            getExecOperations()!!,
            project,
            logger
        )
    }

    fun gitPushTag(remote: String, tag: String) {
        runGitCommand(
            listOf("git", "push", remote, "tag", tag),
            "Unable to push release tag to remote.",
            getExecOperations()!!,
            project,
            logger
        )
    }
}

public fun runGitCommand(
    command: List<String>,
    errorMessage: String,
    execOperations: ExecOperations,
    project: Project,
    logger: Logger,
    failOnError: Boolean = true
) {
    val output = ByteArrayOutputStream()
    val error = ByteArrayOutputStream()

    val result = execOperations.exec { spec: ExecSpec ->
        spec.workingDir = project.projectDir
        spec.commandLine = command
        spec.setStandardOutput(output)
        spec.setErrorOutput(error)
        spec.setIgnoreExitValue(true)
    }

    if (result.exitValue != 0) {
        val errorStr = error.toString(StandardCharsets.UTF_8)
        logger.info("$ERROR_PREFIX $errorStr")
        val extraInfo = if (errorStr.contains("fatal: unable to access")) {
            "Please check your connection to git hosting."
        } else {
            ""
        }

        if (failOnError) {
            throw GradleException("$ERROR_PREFIX $errorMessage $extraInfo")
        } else {
            logger.warn("$WARN_PREFIX $errorMessage $extraInfo")
        }
    }
}
