package com.akuleshov7.vercraft.core

import org.eclipse.jgit.api.Git
import java.io.File

public fun getVersion(gitPath: File) {
    Git.open(gitPath).use { git ->
        val releaseBranches = ReleaseBranches(git)
        val version = VersionCalculator(git, releaseBranches, git.repository)
        println(version.calc())
        //releaseBranches.createNewRelease(SemVerReleaseType.MAJOR)
    }
}
