package com.akuleshov7.vercraft

import com.akuleshov7.vercraft.core.Config
import com.akuleshov7.vercraft.core.DefaultConfig
import com.akuleshov7.vercraft.core.getVersion
import com.akuleshov7.vercraft.utils.GitUtilsTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * Task for calculation of the version for the commit which is checked out now.
 * In case there were no releases in main/master branch before, then it will be calculated with patch version
 * (counting from the first commit in main).
 */
abstract class GitVersionTask : GitUtilsTask() {
    @Internal
    var version: String = "0.0.0"

    @get:Input
    abstract val config: Property<Config>

    @TaskAction
    fun gitVersion() {
        project.beforeEvaluate {
            version = getVersion(project.projectDir, DefaultConfig)
            project.allprojects.forEach {
                it.version = version
            }
        }
    }
}
