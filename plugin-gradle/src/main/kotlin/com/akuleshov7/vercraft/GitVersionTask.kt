package com.akuleshov7.vercraft

import com.akuleshov7.vercraft.core.getVersion
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * Task for calculation of the version for the commit which is checked out now.
 * In case there were no releases in main/master branch before, then it will be calculated with patch version
 * (counting from the first commit in main).
 */
abstract class GitVersionTask : DefaultTask() {
    @Internal
    var version: String = "0.0.0"

    @TaskAction
    fun gitVersion() {
        project.beforeEvaluate {
            version = getVersion(project.projectDir)
            project.allprojects.forEach {
                it.version = version
            }
        }
    }
}
