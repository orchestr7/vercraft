package com.akuleshov7.vercraft.core

import org.eclipse.jgit.api.Git
import java.io.File


public fun getVersion(gitPath: File): String {
    Git.open(gitPath).use { git ->
        val releaseBranches = ReleaseBranches(git)
        val version = VersionCalculator(git, releaseBranches, git.repository)
        val resultedVer = version.calc()
        // FixMe: logging
        return resultedVer
    }
}

public fun createRelease(gitPath: File, semVerReleaseType: SemVerReleaseType) {
    Git.open(gitPath).use { git ->
        ReleaseBranches(git).createNewRelease(semVerReleaseType)
    }
}

public fun createRelease(gitPath: File, version: SemVer) {
    Git.open(gitPath).use { git ->
        ReleaseBranches(git).createNewRelease(version)
    }
}
