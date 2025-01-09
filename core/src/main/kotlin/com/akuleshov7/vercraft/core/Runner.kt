package com.akuleshov7.vercraft.core

import org.apache.logging.log4j.LogManager
import org.eclipse.jgit.api.Git
import java.io.File

private val logger = LogManager.getLogger()

public fun getVersion(gitPath: File): String {
    Git.open(gitPath).use { git ->
        val releases = Releases(git)
        val resultedVer = releases.version.calc()
        logger.warn("VERsion CRAFTED: $resultedVer")
        return resultedVer.toString()
    }
}

public fun createRelease(gitPath: File, semVerReleaseType: SemVerReleaseType): String {
    Git.open(gitPath).use { git ->
        val version = Releases(git).createNewRelease(semVerReleaseType)
        logger.warn("Successfully \"VerCrafted\" the release [$version]")
        return version
    }
}

// TODO: support explicit version creation in Gradle Plugin
public fun createRelease(gitPath: File, version: SemVer) {
    Git.open(gitPath).use { git ->
        Releases(git).createNewRelease(version)
    }
}


public fun main() {
    getVersion(File("../data-platform"))
}