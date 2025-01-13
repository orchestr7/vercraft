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
internal fun String.shortName() =
    Repository.shortenRefName(this).substringAfterLast(Constants.DEFAULT_REMOTE_NAME + "/")

internal fun String.removeReleasePrefix() = this.substringAfterLast("$RELEASE_PREFIX/")
internal fun String.hasReleasePrefix() = this.startsWith("$RELEASE_PREFIX/")

public data class ReleaseBranch(
    val version: SemVer,
    val branch: Branch,
) {
    public constructor(branch: Branch) : this(SemVer(branch.ref.name.shortName()), branch)
}

/**
 * Class which contains and stores all release branches like release/x.x.x
 * As utility it can:
 * - create new release branches (in case when needed)
 */
public class Releases public constructor(private val git: Git) {
    private val logger = LogManager.getLogger(Releases::class.java)

    init {
        try {
            git.fetch().call()
        } catch (e: TransportException) {
            logger.warn("$ERROR_PREFIX Not able to fetch remote repository <${e.message}>, will proceed with local snapshot.")
        }
    }

    private val repo: Repository = git.repository

    public val mainBranch: Branch = Branch(git, repo.findRef(MAIN_BRANCH_NAME))

    public val releaseBranches: MutableSet<ReleaseBranch> = findReleaseBranches()

    private val currentCheckoutBranch = repo.branch
        ?.let { Branch(git, repo.findRef(it)) }
        ?: run {
            logger.warn(
                "$ERROR_PREFIX your current HEAD is detached (no branch is checked out). " +
                        "Usually this happens on CI platforms, which check out particular commit. " +
                        "Please pass the branch which you are trying to process to VerCraftю"
            )

            throw NullPointerException("Current checked out branch is null (looks like your current HEAD is detached)")
        }

    public val version: VersionCalculator = VersionCalculator(git, this, currentCheckoutBranch)

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
            .also { releaseBranches.add(ReleaseBranch(Branch(git, it))) }

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
            .groupBy { it.branch.ref.name.shortName() }

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
                val branchName = it.name.shortName()
                branchName.hasReleasePrefix() && branchName.removeReleasePrefix().isValidSemVerFormat()
            }
            .map { ReleaseBranch(Branch(git, it)) }
            .toHashSet()

    // === TODO: unused logic, which should be unified with the default gradle cmd tasks
    // check [[MakeReleaseTask]]
    private fun pushBranch(newVersion: SemVer) {
        try {
            git.push()
                .setCredentialsProvider(LocalCredentialsProvider)
                .add("release/$newVersion")
                .call()

            logger.warn("+ Pushed a branch [release/$newVersion]")
        } catch (e: TransportException) {
            logger.warn("$ERROR_PREFIX Not able to push branch to remote repository <${e.message}>, please do it manually.")
        }
    }

    private fun pushTag(newVersion: SemVer) {
        try {
            git.push()
                .setCredentialsProvider(LocalCredentialsProvider)
                .add("refs/tags/v$newVersion")
                .call()

            logger.warn("+ Pushed a tag v$newVersion [Release $newVersion]")
        } catch (e: TransportException) {
            logger.warn("$ERROR_PREFIX Not able to push tag to remote repository <${e.message}>, please do it manually.")
        }
    }
}
