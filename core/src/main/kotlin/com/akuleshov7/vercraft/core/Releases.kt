package com.akuleshov7.vercraft.core

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository

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

    private val repo: Repository = git.repository

    public val mainBranch: Branch = Branch(git, repo.findRef(MAIN_BRANCH_NAME))

    public val releaseBranches: HashSet<ReleaseBranch> = git.branchList().call()
        .toHashSet()
        .filter { it.name.hasReleasePrefix() && it.name.isValidSemVerFormat() }
        .map { ReleaseBranch(Branch(git, it)) }
        .toHashSet()

    private val currentCheckoutBranch = Branch(git, repo.findRef(repo.branch))

    public val version: VersionCalculator = VersionCalculator(git, this, currentCheckoutBranch)

    public fun isReleaseBranch(branch: Branch): Boolean = releaseBranches.find { it.branch == branch } != null

    public fun getLatestReleaseBranch(): ReleaseBranch? =
        releaseBranches.maxByOrNull { it.version }

    public fun createNewRelease(releaseType: SemVerReleaseType): String {
        val newVersion = getLatestReleaseBranch()
            ?.version
            ?.nextVersion(releaseType)
            ?: version.calc().nextVersion(releaseType)

        if (currentCheckoutBranch != mainBranch) {
            throw IllegalStateException(
                "(!) Branch which is currently checked out is [${currentCheckoutBranch.ref.name}], " +
                        "but ${releaseType.name} release should be done from [${mainBranch.ref.name}] branch. " +
                        "Because during the release VerCraft will create a new branch and tag."
            )
        } else {
            if (releaseType == SemVerReleaseType.PATCH) {
                logger.warn(
                    "(!) ReleaseType PATCH has been selected, so no new release branches " +
                            "will be created as patch releases should be made only in existing release branch."
                )
                // FIXME: need to switch to latest release branch and latest commit and set release tag there
            } else {
                createBranch(newVersion)
                createTag(newVersion)
            }
        }
        return "$newVersion"
    }

    public fun createNewRelease(version: SemVer): String {
        if (releaseBranches.map { it.version }.contains(version)) {
            logger.warn(
                "(!) The branch with the version [$version] which was selected for " +
                        "the new release already exists. No branches will be created, please change the version."
            )

        } else {
            createBranch(version)
            createTag(version)
        }
        return version.toString()
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
