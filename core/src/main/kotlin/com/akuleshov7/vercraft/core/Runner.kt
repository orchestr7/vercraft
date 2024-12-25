package com.akuleshov7.vercraft.core

import org.eclipse.jgit.api.Git
import java.io.File


public fun getVersion(gitPath: File): String {
    Git.open(gitPath).use { git ->
        val releases = Releases(git)
        val resultedVer = releases.version.calc()
        // FixMe: logging
        return resultedVer.toString()
    }
}

public fun createRelease(gitPath: File, semVerReleaseType: SemVerReleaseType): String {
    Git.open(gitPath).use { git ->
        return Releases(git).createNewRelease(semVerReleaseType)
    }
}

public fun createRelease(gitPath: File, version: SemVer) {
    Git.open(gitPath).use { git ->
        Releases(git).createNewRelease(version)
    }
}
