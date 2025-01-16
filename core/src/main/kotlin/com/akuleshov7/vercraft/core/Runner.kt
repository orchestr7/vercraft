package com.akuleshov7.vercraft.core

import org.apache.logging.log4j.LogManager
import org.eclipse.jgit.api.Git
import java.io.File

private val logger = LogManager.getLogger()

public fun getVersion(gitPath: File, config: Config): String {
    Git.open(gitPath).use { git ->
        val releases = Releases(git, config)
        val resultedVer = releases.version.calc()
        logger.warn(">> VerCrafted: $resultedVer")
        return resultedVer.toString()
    }
}

public fun createRelease(gitPath: File, semVerReleaseType: SemVerReleaseType, config: Config): String {
    Git.open(gitPath).use { git ->
        val version = Releases(git, config).createNewRelease(semVerReleaseType)
        logger.warn(">> VerCrafted new release [$version]")
        return version
    }
}

public fun createRelease(gitPath: File, version: SemVer, config: Config): String {
    Git.open(gitPath).use { git ->
        Releases(git, config).createNewRelease(version)
        logger.warn(">> VerCrafted new release [$version]")
    }

    return version.toString()
}
