package com.akuleshov7.vercraft.core

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

internal const val MAIN_BRANCH_NAME: String = "main"

public class VersionCalculator(
    git: Git,
    private val releases: Releases,
    private val currentCheckoutBranch: Branch,
) {
    private val repo: Repository = git.repository
    private val headCommit = repo.resolve("HEAD")

    public fun calc(): SemVer =
        when {
            currentCheckoutBranch.ref.name.shortName() == MAIN_BRANCH_NAME -> calcVersionInMain()
            releases.isReleaseBranch(currentCheckoutBranch) -> calcVersionInRelease()
            else -> calcVersionInBranch()
        }

    /**
     * The [[currentCheckoutBranch]] refers to the main branch in this context.
     * To determine the version, we need to identify the last release and calculate
     * the next version incrementally. For example:
     *
     * aaa -> bbb (release/0.1.0) -> xxx -> yyy (?)
     *
     * The version for commit yyy is derived by adding the number of commits between
     * commits bbb and yyy (in this case, 2 commits) to the incremented MINOR version of the
     * last release version (0.1.0). Therefore, the resulting version for (yyy) will be 0.2.1.
     *
     * (!) Additionally, the version will be appended with a postfix consisting of
     * the first 5 characters of the commit hash and "-rc". So the result will be as following:
     * aaa -> bbb (release/0.1.0) -> 0.2.0-hash-rc -> 0.2.1-hash-rc (?)
     */
    private fun calcVersionInMain(): SemVer {
        val latestRelease = releases.getLatestReleaseBranch()
        // if no releases were made so far, then will calculate version starting from the initial commit
        val baseCommit = latestRelease?.branch
            ?.findBaseCommitIn(currentCheckoutBranch) ?: currentCheckoutBranch.gitLog[0]
        val distance = currentCheckoutBranch.numberOfCommitsAfter(baseCommit)

        val shortedHashCode = baseCommit.name.substring(0, 5)

        return latestRelease
            ?.let {
                latestRelease.version
                    .nextVersion(SemVerReleaseType.MINOR)
                    .incrementPatchVersion(distance)
                    .setPostFix("rc+$shortedHashCode")
            }
            ?: run { SemVer(0, 0, distance) }
    }

    /**
     * The [[currentCheckoutBranch]] refers to any "release/" branch in this context.
     *
     * We have checked out a specific commit (HEAD) from a release branch and need to calculate its version.
     * To achieve this, we must identify the first commit in the release branch and calculate the version based on that.
     *
     * Example:
     * aaa -> bbb (release/0.1.0)
     *        V
     *       xxx
     *        V
     *       yyy (?)
     *
     * To compute the version, we locate the initial commit in the release branch — the point where the branch diverged
     * from the main branch. Then, we calculate the distance (number of commits) between this base commit and the
     * current commit for which we are determining the version.
     *
     * In this example:
     * - Commit "bbb" represents version "0.1.0".
     * - Commit "xxx" will have version "0.1.1".
     * - Commit "yyy" will be assigned version "0.1.2".
     *
     * This scenario applies to PATCH versions. Commits like **xxx** and **yyy** are typically created to implement
     * patches or additional fixes after the release of version **0.1.0** (commit **bbb**).
     */
    private fun calcVersionInRelease(): SemVer {
        val distance = distanceFromMainBranch()
        return releases.releaseBranches.find { it.branch == currentCheckoutBranch }
            ?.version
            ?.incrementPatchVersion(distance)
            ?: throw IllegalStateException(
                "Cannot find branch ${currentCheckoutBranch.ref.name} in the list of release branches:" +
                        "${releases.releaseBranches}"
            )
    }

    /**
     * The [[currentCheckoutBranch]] refers to any non-release branch (e.g., "feature/blabla") in this context.
     *
     * To calculate the commit version, we determine the distance (number of commits) between the base commit
     * (where the branch diverged from the main branch) and the current commit. This distance is used as the PATCH version,
     * while the MAJOR and MINOR versions are set to 0 (resulting in a version format of 0.0.x).
     *
     * (!) Additionally, the version will include a postfix composed of:
     * - The last segment of the branch name (substring after the final "/").
     * - The current date in "yyyy-MM-dd" format.
     *
     * For example, if the branch is "feature/blabla" and the commit distance is 1,
     * the version will be: "0.0.1-blabla-2024-12-24".
     */
    private fun calcVersionInBranch(): SemVer {
        // replace all special symbols except letters and digits from branch name and limit it to 10 symbols
        val branchName = currentCheckoutBranch.ref.name.substringAfterLast("/")
        val branch = branchName
            .substring(0, min(branchName.length, 10))
            .replace("[^A-Za-z0-9]".toRegex(), "")

        val distance = distanceFromMainBranch()

        RevWalk(repo).use { walk ->
            val commit: RevCommit = walk.parseCommit(headCommit)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
            dateFormat.timeZone = TimeZone.getDefault()
            val formattedDate = dateFormat.format(Date(commit.commitTime * 1000L))

            return SemVer(0, 0, distance + 1).setPostFix("$branch+$formattedDate")
        }
    }

    private fun distanceFromMainBranch(): Int {
        val baseCommit = currentCheckoutBranch.findBaseCommitIn(releases.mainBranch)
            ?: throw IllegalStateException(
                "Can't find common ancestor commits between $MAIN_BRANCH_NAME " +
                        "and ${currentCheckoutBranch.ref.name} branches. Looks like these branches have no relation " +
                        "and that is inconsistent git state."
            )
        return currentCheckoutBranch.numberOfCommitsAfter(baseCommit)
    }
}
