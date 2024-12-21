package com.akuleshov7.vercraft.core

import org.eclipse.jgit.api.Git

public const val REFS_HEADS: String = "refs/heads"
internal const val RELEASE_PREFIX = "release"
private fun String.removeReleaseBranchPrefix() = this.removePrefix("$REFS_HEADS/$RELEASE_PREFIX/")

public data class ReleaseBranch(
    val version: SemVer,
    val branch: Branch,
) {
    public constructor(branch: Branch) : this(SemVer(branch.ref.name.removeReleaseBranchPrefix()), branch)
}

/**
 * Class which contains and stores all release branches like release/x.x.x
 * As utility it can:
 * - create new release branches (in case when needed)
 */
public class ReleaseBranches public constructor(private val git: Git) {
    public val set: HashSet<ReleaseBranch> = git.branchList().call()
        .toHashSet()
        .filter {
            it.name.startsWith("$REFS_HEADS/$RELEASE_PREFIX/") &&
                    it.name.removeReleaseBranchPrefix().isValidSemVerFormat()
        }
        .map { ReleaseBranch(Branch(git, it)) }
        .toHashSet()

    public fun findBranch(branch: Branch): ReleaseBranch? = set.find { it.branch == branch }

    public fun getLatestReleaseBranch(): ReleaseBranch? =
        set.maxByOrNull { it.version }

    public fun createNewRelease(releaseType: SemVerReleaseType) {
        if (releaseType == SemVerReleaseType.PATCH) {
            // FixMe: logging
            return
        }

        val newVersion = getLatestReleaseBranch()
            ?.version
            ?.nextVersion(releaseType)
            // if no branches have been yet created, then we can use version 0,0,0 as the default one
            // we do not need to calculate anything here, as there is no possible situation when release
            // was created with PATCH version. The ideology of this project says that patch release can only be made
            // by the commit in the release branch with MAJOR or MINOR version
            ?: SemVer(0, 0, 0).nextVersion(releaseType)

        createBranch(newVersion)
    }

    public fun createNewRelease(version: SemVer) {
        if (set.map { it.version }.contains(version)) {
            // FixMe: logging
            return
        }
        createBranch(version)
    }

    private fun createBranch(newVersion: SemVer) {
        git.branchCreate()
            .setName("release/$newVersion")
            .call()
            .also { set.add(ReleaseBranch(Branch(git, it))) }

        // creating annotated tags
        git.tag()
            .setName("$newVersion")
            .setMessage("Release $newVersion")
            .call()
    }
}
