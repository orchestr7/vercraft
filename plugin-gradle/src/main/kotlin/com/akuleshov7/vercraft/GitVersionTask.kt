package com.akuleshov7.vercraft

import com.akuleshov7.vercraft.core.getVersion
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class GitVersionTask : DefaultTask() {
    @Internal
    var version: String = "0.0.0"

    @TaskAction
    fun gitVersion() {
        version = getVersion(project.projectDir)
        project.version = version
    }
}
