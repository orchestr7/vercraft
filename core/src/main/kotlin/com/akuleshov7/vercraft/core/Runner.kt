package com.akuleshov7.vercraft.core

import org.eclipse.jgit.api.Git
import java.io.File

public fun gitVersion(gitPath: File, config: Config): String {
    Git.open(gitPath).use { git ->
        val releases = Releases(git, config)
        val resultedVer = releases.version.calc()
        println(">> VerCrafted: $resultedVer")
        return resultedVer.toString()
    }
}

public fun makeRelease(gitPath: File, semVerReleaseType: SemVerReleaseType, config: Config): String {
    Git.open(gitPath).use { git ->
        val version = Releases(git, config).createNewRelease(semVerReleaseType)
        println(">> VerCrafted new release [$version]")
        return version
    }
}

public fun makeRelease(gitPath: File, version: SemVer, config: Config): String {
    Git.open(gitPath).use { git ->
        Releases(git, config).createNewRelease(version)
        println(">> VerCrafted new release [$version]")
    }

    return version.toString()
}
