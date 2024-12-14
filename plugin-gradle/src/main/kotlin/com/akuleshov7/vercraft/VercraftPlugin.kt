package com.akuleshov7.vercraft

import com.akuleshov7.vercraft.core.getVersion
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class GitVersionTask : DefaultTask() {
    @get:Input
    abstract val fileText: Property<String>

    @TaskAction
    fun action() {
        getVersion(project.projectDir)
    }
}

class VercraftPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("gitVersion", GitVersionTask::class.java) {
            it.fileText.set("HERE WILL BE A PATH TO A REPO")
        }
    }
}
