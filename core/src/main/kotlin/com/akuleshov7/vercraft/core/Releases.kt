package com.akuleshov7.vercraft.core

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.eclipse.jgit.api.Git

public const val REFS_HEADS: String = "refs/heads"
internal const val RELEASE_PREFIX = "release"
internal fun String.removePrefix() = this.substringAfter("$REFS_HEADS/$RELEASE_PREFIX/")
internal fun String.hasReleasePrefix() = this.startsWith("$REFS_HEADS/$RELEASE_PREFIX/")

public data class ReleaseBranch(
    val version: SemVer,
    val branch: Branch,
) {
    public constructor(branch: Branch) : this(SemVer(branch.ref.name.removePrefix()), branch)
}

/**
 * Class which contains and stores all release branches like release/x.x.x
 * As utility it can:
 * - create new release branches (in case when needed)
 */
public class Releases public constructor(private val git: Git) {
    private val logger = LogManager.getLogger(Releases::class.java)

    public val releaseBranches: HashSet<ReleaseBranch> = git.branchList().call()
        .toHashSet()
        .filter { it.name.hasReleasePrefix() && it.name.isValidSemVerFormat() }
        .map { ReleaseBranch(Branch(git, it)) }
        .toHashSet()

    private val currentCheckoutBranch = Branch(git, git.repository.findRef(git.repository.branch))

    public val version: VersionCalculator = VersionCalculator(git, this, currentCheckoutBranch)

    public fun isReleaseBranch(branch: Branch): Boolean = releaseBranches.find { it.branch == branch } != null

    public fun getLatestReleaseBranch(): ReleaseBranch? =
        releaseBranches.maxByOrNull { it.version }

    public fun createNewRelease(releaseType: SemVerReleaseType): String {
        val newVersion = getLatestReleaseBranch()
            ?.version
            ?.nextVersion(releaseType)
            ?: version.calc().nextVersion(releaseType)

        if (releaseType == SemVerReleaseType.PATCH) {
            // no need to create branch for PATCH release, but anyway need to create a TAG
            logger.warn("(!) ReleaseType PATCH has been selected, so no new release branches " +
                    "will be created as patch releases should be made only in existing release branch.")
            // FIXME: need to switch to latest release branch and latest commit and set release tag there
        } else {
            // FixMe: should only be done from the latest MAIN
            createBranch(newVersion)
            createTag(newVersion)
        }

        return "$newVersion"
    }

    public fun createNewRelease(version: SemVer) {
        if (releaseBranches.map { it.version }.contains(version)) {
            logger.warn("(!) The branch with the version [$version] which was selected for the new release already exists. " +
                    "No branches will be created, please change the version.")
            return
        }
        createBranch(version)
        createTag(version)
    }

    private fun createTag(newVersion: SemVer) {
        git.tag()
            .setName(newVersion.toString())
            .setMessage("Release $newVersion")
            .call()

        logger.warn("+ Created a tag [Release $newVersion]")
    }

    private fun createBranch(newVersion: SemVer) {
        git.branchCreate()
            .setName("release/$newVersion")
            .call()
            .also { releaseBranches.add(ReleaseBranch(Branch(git, it))) }

        logger.warn("+ Created a branch [release/$newVersion]")
    }
}
