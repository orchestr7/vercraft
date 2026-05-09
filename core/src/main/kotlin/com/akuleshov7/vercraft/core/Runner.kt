package com.akuleshov7.vercraft.core

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File

private fun openGit(gitPath: File): Git {
    val builder = FileRepositoryBuilder().findGitDir(gitPath).setMustExist(true)
    requireNotNull(builder.gitDir) {
        "No git repository found at or above: $gitPath"
    }
    return Git(builder.build(), true)
}

public fun gitVersion(gitPath: File, config: Config): String {
    openGit(gitPath).use { git ->
        val releases = Releases(git, config)
        val resultedVer = releases.version.calc()
        println(">> VerCrafted: $resultedVer")
        return resultedVer.toString()
    }
}

public fun makeRelease(gitPath: File, semVerReleaseType: SemVerReleaseType, config: Config): SemVer {
    openGit(gitPath).use { git ->
        val version = Releases(git, config).createNewRelease(semVerReleaseType)
        println(">> VerCrafted new release [$version]")
        return version
    }
}

public fun makeRelease(gitPath: File, version: SemVer, config: Config): SemVer {
    openGit(gitPath).use { git ->
        Releases(git, config).createNewRelease(version)
        println(">> VerCrafted new release [$version]")
    }
    return version
}