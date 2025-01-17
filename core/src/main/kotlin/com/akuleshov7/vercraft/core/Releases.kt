package com.akuleshov7.vercraft.core

import com.akuleshov7.vercraft.core.utils.ERROR_PREFIX
import org.apache.logging.log4j.LogManager
import org.eclipse.jgit.api.ListBranchCommand.ListMode.REMOTE
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository

internal const val RELEASE_PREFIX = "release"

/**
 * Removal of prefixes: "refs/heads/", "refs/remotes/origin", "refs/tags/".
 * We do not care if someone has a branch name 'origin', it will be filtered later, we only care about release/X.X.X branches.
 *
 * TODO: add configuration for remotes other than 'origin'
 */
internal fun String.shortName(remoteName: String) =
    Repository.shortenRefName(this).substringAfterLast("$remoteName/")

internal fun String.removeReleasePrefix() = this.substringAfterLast("$RELEASE_PREFIX/")
internal fun String.hasReleasePrefix() = this.startsWith("$RELEASE_PREFIX/")

public data class ReleaseBranch(
    val version: SemVer,
    val branch: Branch,
) {
    // TODO: change to config value here instead of Constants
    public constructor(branch: Branch, config: Config) : this(
        SemVer(branch.ref.name.shortName(config.remote)),
        branch
    )
}

/**
 * Class which contains and stores all release branches like release/x.x.x
 * As utility it can:
 * - create new release branches (in case when needed)
 */
public class Releases public constructor(private val git: Git, private val config: Config) {
    private val logger = LogManager.getLogger(Releases::class.java)

    private val repo: Repository = git.repository

    public val mainBranch: Branch = findBranch(config.defaultMainBranch)
        ?: throw IllegalStateException("${config.defaultMainBranch} branch cannot be found in current git repo. " +
                "Please check your fetched branches.")

    public val releaseBranches: MutableSet<ReleaseBranch> = findReleaseBranches()

    private val currentCheckoutBranch = findBranch(repo.branch)
        ?: run {
            logger.warn(
                "$ERROR_PREFIX your current HEAD is detached (no branch is checked out). " +
                        "Usually this happens on CI platforms, which check out particular commit. " +
                        "Trying to resolve branch name using known CI ENV variables: " +
                        "$GITLAB_BRANCH_REF, $GITHUB_HEAD_REF, $BITBUCKET_BRANCH."
            )

            val branchName = config.checkoutBranch
                ?: System.getenv(GITLAB_BRANCH_REF)
                ?: System.getenv(GITHUB_HEAD_REF)
                ?: System.getenv(BITBUCKET_BRANCH)
                ?: System.getenv(VERCRAFT_BRANCH)
                ?: run {
                    logger.warn(
                        "$ERROR_PREFIX following variables are not defined in current env" +
                                "$GITLAB_BRANCH_REF, $GITHUB_HEAD_REF, $BITBUCKET_BRANCH" +
                                "Please pass the branch name which you are trying to process now explicitly " +
                                "to VerCraft by setting ENV variable \$VERCRAFT_BRANCH. "
                    )
                    throw NullPointerException(
                        "Current HEAD is detached and CI env variables with the branch name are not set, so" +
                                "not able to determine the original branch name."
                    )
                }

            Branch(
                git,
                git.branchList()
                    .setListMode(REMOTE)
                    .call()
                    .first { it.name.endsWith(branchName) }
            )
        }

    public val version: VersionCalculator = VersionCalculator(git, config, this, currentCheckoutBranch)

    public fun isReleaseBranch(branch: Branch): Boolean = releaseBranches.find { it.branch == branch } != null

    public fun getLatestReleaseBranch(): ReleaseBranch? =
        releaseBranches.maxByOrNull { it.version }

    // TODO: Not to create a release if we are now on main and on this commit there is already a release branch made
    public fun createNewRelease(releaseType: SemVerReleaseType): String {
        val newVersion = getLatestReleaseBranch()
            ?.version
            ?.nextVersion(releaseType)
            ?: version.calc().nextVersion(releaseType)

        if (currentCheckoutBranch != mainBranch) {
            throw IllegalStateException(
                "$ERROR_PREFIX Branch which is currently checked out is [${currentCheckoutBranch.ref.name}], " +
                        "but ${releaseType.name} release should always be done from [${mainBranch.ref.name}] branch. " +
                        "Because during the release VerCraft will create a new branch and tag."
            )
        } else {
            if (releaseType == SemVerReleaseType.PATCH) {
                logger.warn(
                    "$ERROR_PREFIX ReleaseType PATCH has been selected, so no new release branches " +
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
                "$ERROR_PREFIX The branch with the version [$version] which was selected for " +
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
            .setName("v${newVersion}")
            .setMessage("Release $newVersion")
            .call()

        logger.warn("+ Created a tag v$newVersion [Release $newVersion]")
    }

    private fun createBranch(newVersion: SemVer) {
        git.branchCreate()
            .setName("release/$newVersion")
            .call()
            .also { releaseBranches.add(ReleaseBranch(Branch(git, it), config)) }

        logger.warn("+ Created a branch [release/$newVersion]")
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
            .groupBy { it.branch.ref.name.shortName(config.remote) }

        allReleaseBranches.keys.forEach {
            val value = allReleaseBranches[it]
            if (value!!.size > 1) {
                if (value[0].branch.gitLog != value[1].branch.gitLog) {
                    // TODO: error when release branch is checked-out (and calculating version for it) and differs from remote
                    logger.warn(
                        "$ERROR_PREFIX Remote and local branches '$it' differ. " +
                                "Do you have any unpublished changes in your local branch? Will use " +
                                "local branch to calculate versions."
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
                val branchName = it.name.shortName(config.remote)
                branchName.hasReleasePrefix() && branchName.removeReleasePrefix().isValidSemVerFormat()
            }
            .map { ReleaseBranch(Branch(git, it), config) }
            .toHashSet()

    /**
     * It appeared that standard findRef is only checking local branches
     *
     */
    public fun findBranch(branch: String?): Branch? {
        if (branch == null) return null

        val foundBranch = repo.findRef("${Constants.R_HEADS}$branch")
            ?: repo.findRef("${Constants.R_REMOTES}${config.remote}/$branch")
            ?: run {
                logger.error(
                    "$ERROR_PREFIX Cannot find $branch in current repository. " +
                            "Please check that fetch depth is not set to 1."
                )
                return null
            }

        return Branch(git, foundBranch)
    }
}
