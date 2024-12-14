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
    public val list: MutableList<ReleaseBranch> = git.branchList().call()
        .filter { it.name.startsWith("$REFS_HEADS/$RELEASE_PREFIX/") }
        .filter { it.name.removeReleaseBranchPrefix().isValidSemVerFormat() }
        .map { ReleaseBranch(Branch(git, it)) }
        .toMutableList()

    public fun findBranch(branch: Branch): ReleaseBranch? = list.find { it.branch == branch }

    public fun getLatestReleaseBranch(): ReleaseBranch? =
        list.maxByOrNull { it.version }

    public fun createNewRelease(releaseType: SemVerReleaseType, version: SemVer? = null) {
        // if version is null - then need to calculate it from other existing release branches
        val newVersion = version ?: run {
            getLatestReleaseBranch()
                ?.version
                ?.nextVersion(releaseType)
                ?: SemVer(0, 0, 0).nextVersion(releaseType)
        }
        git.branchCreate()
            .setName("release/$newVersion")
            .call()
            .also { list.add(ReleaseBranch(Branch(git, it))) }

        // creating annotated tags
        git.tag()
            .setName("$newVersion")
            .setMessage("Release $newVersion")
            .call()
    }
}
