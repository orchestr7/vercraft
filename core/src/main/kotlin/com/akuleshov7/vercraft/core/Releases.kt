package com.akuleshov7.vercraft.core

import com.akuleshov7.vercraft.core.utils.ERROR_PREFIX
import com.akuleshov7.vercraft.core.utils.WARN_PREFIX
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.ListBranchCommand.ListMode.REMOTE
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk

internal const val RELEASE_PREFIX = "release"

internal const val WARN_BRANCH_NAME = "$WARN_PREFIX FYI: your current HEAD is detached (no branch is checked out). " +
        "Usually this happens on CI platforms, which check out particular commit. " +
        "Vercraft will try to resolve branch name using known CI ENV variables: " +
        "$GITLAB_BRANCH_REF, $GITHUB_HEAD_REF, $BITBUCKET_BRANCH. " +
        "You can also set it explicitly by $VERCRAFT_BRANCH."

internal const val ERROR_MISSING_BRANCH_NAME = "$ERROR_PREFIX following variables are not defined in current env " +
        "$GITLAB_BRANCH_REF, $GITHUB_HEAD_REF, $BITBUCKET_BRANCH " +
        "Please pass the branch name which you are trying to process (check-out) now explicitly " +
        "to VerCraft by setting ENV variable \$VERCRAFT_BRANCH. "

internal const val ERROR_BRANCH_DETECTION = "Current HEAD is detached and CI env variables with " +
        "the branch name are not set, so not able to determine the original branch name and calculate the version."

/**
 * Removal of prefixes: "refs/heads/", "refs/remotes/origin", "refs/tags/".
 * We do not care if someone has a branch name 'origin', it will be filtered later, we only care about release/X.X.X branches.
 *
 */
internal fun String.shortName(remoteName: String) =
    Repository.shortenRefName(this).substringAfterLast("$remoteName/")

internal fun String.removeReleasePrefix() = this.substringAfterLast("$RELEASE_PREFIX/")
internal fun String.hasReleasePrefix() = this.startsWith("$RELEASE_PREFIX/")

public class ReleaseBranch(
    git: Git,
    config: Config,
    ref: Ref,
    defaultMainBranch: Branch?
) : Branch(git, ref, defaultMainBranch) {
    public val version: SemVer = SemVer(ref.name.shortName(config.remote.value))
}

/**
 * Class which contains and stores all release branches like release/x.x.x
 * As utility it can:
 * - create new release branches (in case when needed)
 */
public class Releases public constructor(private val git: Git, private val config: Config) {
    public val repo: Repository = git.repository

    public val defaultMainBranch: Branch = Branch(git, config, config.defaultMainBranch.value).also {
        it.ref ?: throw IllegalStateException(
            "${config.defaultMainBranch.value} branch cannot be found in current git repo. " +
                    "Please set defaultMainBranch and check your fetched branches and fetch-depth " +
                    "(CI platforms usually limit it)."
        )
    }

    public val releaseBranches: MutableSet<ReleaseBranch> = findReleaseBranches()

    private val currentBranch = setCurrentBranch()

    public val version: VersionCalculator = VersionCalculator(git, config, this, currentBranch)

    private val currentCommit: RevCommit? = RevWalk(repo).use { it.parseCommit(repo.resolve("HEAD")) }

    public fun isReleaseBranch(branch: Branch): Boolean =
        releaseBranches.find { it.ref == branch.ref } != null

    /**
     * Method helps to find the closest release branch before [commit] in [branch].
     * For example, if we have releases R1 and R2, and we are trying to fing latest release for commit C:
     * A - B - C - D - E -> [branch]
     *     |       |
     *     R1      R2
     *
     * The answer will be a branch R1 and commit B.
     */
    public fun getLatestReleaseForCommit(commit: RevCommit, branch: Branch): ReleaseBranch? {
        var foundCommit = false
        branch.gitLog.forEach { commitIterator ->
            if(commit.name == commitIterator.name) foundCommit = true

            if (foundCommit) {
                val res = releaseBranches.find { it.baseCommitInMain?.name == commitIterator.name }
                if(res != null) return res }
            }

        if (!foundCommit) throw IllegalArgumentException("Commit $commit cannot be found in branch ${branch.ref?.name}")

        return null
    }

    public fun getLatestReleaseBranch(): ReleaseBranch? =
        releaseBranches.maxByOrNull { it.version }

    // TODO: add tests to cover makeRelease task
    public fun createNewRelease(releaseType: SemVerReleaseType): String {
        val latestRelease = getLatestReleaseBranch()

        val newVersion = latestRelease
            ?.version
            ?.nextVersion(releaseType)
            ?: version.calc().nextVersion(releaseType)

        when {
            currentBranch != defaultMainBranch ->  throw IllegalStateException(
                "$ERROR_PREFIX Branch which is currently checked out is [${currentBranch.ref?.name}], " +
                        "but ${releaseType.name} release should always be done from default [${defaultMainBranch.ref?.name}] " +
                        "branch. This is required, because during the release VerCraft will create a " +
                        "new branch and tag from the default branch."
            )

            releaseBranches.map { it.baseCommitInMain }.contains(currentCommit) ->
                throw IllegalStateException(
                    "$ERROR_PREFIX Current checked-out commit already is associated with release branch " +
                            "(release branch was already created from this commit). " +
                            "Making a new release from this commit is pointless, please delete or use existing branch."
                )

            else -> {
                if (releaseType == SemVerReleaseType.PATCH) {
                    println(
                        "$WARN_PREFIX ReleaseType PATCH has been selected, so no new release branches " +
                                "will be created, as patch releases should be made only in existing release branch. " +
                                if (latestRelease?.version != null) "Latest release: $latestRelease." else ""
                    )

                    if (latestRelease == null) {
                        // if there have been no releases yet, we will simply create a patch tag on main/master
                        git.checkout().setName(config.defaultMainBranch.value).call()
                        createTag(version.calc())
                    } else {
                        // otherwise - we will check out release branch and tag latest commit
                        git.checkout().setName(latestRelease.ref!!.name).call()
                        // here we need to recreate VersionCalculator, because previously it was created for default branch
                        createTag(VersionCalculator(git, config, this, latestRelease).calc())
                    }

                } else {
                    createBranch(newVersion)
                    createTag(newVersion)
                }
            }
        }

        return "$newVersion"
    }

    public fun createNewRelease(version: SemVer): String {
        if (releaseBranches.map { it.version }.contains(version)) {
            println(
                "$ERROR_PREFIX The branch with the version [$version] which was selected for " +
                        "the new release already exists. No new branches will be created, please select the proper version."
            )
        } else {
            createBranch(version)
            createTag(version)
        }
        return version.toString()
    }

    private fun createTag(newVersion: SemVer) {
        git.tag()
            .setName("v${newVersion.justSemVer()}")
            .setMessage("Release ${newVersion.justSemVer()}")
            .call()

        println(
            "+ Created a tag v${newVersion.justSemVer()} " +
                    "[Release ${newVersion.justSemVer()}], original version is $newVersion"
        )
    }

    /**
     * Getting the name of the current branch name used in PR/MR or currently checked-out.
     * The algorithm is the following:
     * 1) First try to get a checkout branch.
     * 2) If HEAD is detached (no branch is checked-out), then will use env variables set by CI.
     * 3) Try to use VERCRAFT_BRANCH env variable.
     */
    private fun setCurrentBranch(): Branch {
        // repo.branch == null when the HEAD is detached
        return if (repo.branch == null || Branch(git, config, repo.branch, defaultMainBranch).ref == null) {
            println(WARN_BRANCH_NAME)

            val branchName = config.checkoutBranch
                ?.value
                ?: System.getenv(GITLAB_BRANCH_REF)
                ?: System.getenv(GITHUB_HEAD_REF)
                ?: System.getenv(BITBUCKET_BRANCH)
                ?: System.getenv(VERCRAFT_BRANCH)
                ?: run {
                    println(ERROR_MISSING_BRANCH_NAME)
                    throw NullPointerException(ERROR_BRANCH_DETECTION)
                }

            Branch(git, config, branchName, defaultMainBranch).also {
                it.ref ?: throw IllegalArgumentException("Cannot find $branchName in the list branches (remote/local).")
            }
        } else {
            Branch(git, config, repo.branch, defaultMainBranch)
        }
    }

    private fun createBranch(newVersion: SemVer) {
        // new branch is always created with "x" as patch version
        git.branchCreate()
            .setName("release/${newVersion.semVerForNewBranch()}")
            .call()
            .also { releaseBranches.add(ReleaseBranch(git, config, it, defaultMainBranch)) }

        println("+ Created a branch [release/${newVersion.semVerForNewBranch()}]")
    }

    /**
     * We have two sources for release branches: they can be created locally or can be taken from `remotes/origin`
     */
    private fun findReleaseBranches(): MutableSet<ReleaseBranch> {
        // we can use .setListMode(ALL) here to get all release branches, but instead we will take remote and
        // local branches, check their equality and if they are equal, then calculate version
        val releaseBranchesFromRemote = getAndFilterReleaseBranches(git.branchList().setListMode(REMOTE))
        val localReleaseBranches = getAndFilterReleaseBranches(git.branchList().setListMode(null))

        // we will make a union of LOCAL branches and REMOTE, with a priority to LOCAL
        val allReleaseBranches = (localReleaseBranches + releaseBranchesFromRemote)
            .groupBy { it.ref!!.name.shortName(config.remote.value) }

        allReleaseBranches.keys.forEach {
            val value = allReleaseBranches[it]
            if (value!!.size > 1) {
                if (value[0].gitLog != value[1].gitLog) {
                    println(
                        "$ERROR_PREFIX Remote and local branches '$it' differ. " +
                                "Do you have any non-pushed commits in your local branch? Will use " +
                                "remote branch to calculate versions."
                    )
                }
            }
        }

        return allReleaseBranches.values.map { it.first() }.toHashSet()
    }

    private fun getAndFilterReleaseBranches(listBranchCommand: ListBranchCommand) =
        listBranchCommand.call()
            .toHashSet()
            .filter {
                val branchName = it.name.shortName(config.remote.value)
                branchName.hasReleasePrefix() && branchName.removeReleasePrefix().isValidSemVerFormat()
            }
            .map { ReleaseBranch(git, config, it, defaultMainBranch) }
            .toHashSet()
}
