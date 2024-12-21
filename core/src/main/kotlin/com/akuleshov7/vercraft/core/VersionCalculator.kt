package com.akuleshov7.vercraft.core

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import java.text.SimpleDateFormat
import java.util.*

internal const val MAIN_BRANCH_NAME: String = "main"

public class VersionCalculator(
    private val git: Git,
    private val releaseBranches: ReleaseBranches,
    private val repo: Repository
) {
    private val currentCheckoutBranch = Branch(git, repo.findRef(repo.branch))
    private val headCommit = repo.resolve("HEAD")

    public fun calc(): String =
        when {
            currentCheckoutBranch.ref.name == "$REFS_HEADS/$MAIN_BRANCH_NAME" -> "${calcVersionInMain()}"
            releaseBranches.findBranch(currentCheckoutBranch) != null -> "${calcVersionInRelease()}"
            else -> calcVersionInBranch()
        }

    /**
     * [[currentCheckoutBranch]] is main branch in this case,
     * so we need to find last release and calculate the version. For example:
     * 0.0.1 -> 0.0.2 (release/0.1.0) -> xxx -> yyy (?)
     * The version for yyy will be calculated in the following way: 0.1.0 + number of commits between
     * 0.0.2 and yyy (2 commits). So the version should be 0.1.2.
     *
     * (!) We will also add postfix to the version with 5 first letters of commit hash.
     */
    private fun calcVersionInMain(): SemVer {
        val latestRelease = releaseBranches.getLatestReleaseBranch()
        // if no releases were made so far, then will calculate version starting from the initial commit
        val baseCommit = latestRelease?.branch
            ?.findBaseCommitIn(currentCheckoutBranch) ?: currentCheckoutBranch.gitLog[0]
        val distance = currentCheckoutBranch.numberOfCommitsAfter(baseCommit)

        val shortedHashCode = baseCommit.name.substring(0, 5)

        return latestRelease
            ?.let { latestRelease.version.incrementPatchVersion(distance).setPostFix(shortedHashCode) }
            ?: run { SemVer(0, 0, distance + 1) }
    }

    /**
     * We have made a checkout of a (head) commit in release branch ad would like to calculate its version.
     * In that case we need to find first commit in release branch and calculate the version based on that knowledge.
     */
    private fun calcVersionInRelease(): SemVer {
        val mainBranch = Branch(git, repo.findRef(repo.branch))
        val baseCommit = currentCheckoutBranch.findBaseCommitIn(mainBranch)
            ?: throw IllegalStateException(
                "Can't find common ancestor commits between $MAIN_BRANCH_NAME " +
                        "and ${currentCheckoutBranch.ref.name} branches. Looks like these branches have no relation."
            )
        val distance = currentCheckoutBranch.numberOfCommitsAfter(baseCommit)

        return releaseBranches.set.find { it.branch == currentCheckoutBranch }?.version?.incrementPatchVersion(distance)
            ?: throw IllegalStateException(
                "Cannot find branch ${currentCheckoutBranch.ref.name} in the list of release branches:" +
                        "${releaseBranches.set}"
            )
    }

    private fun calcVersionInBranch(): String {
        // replace all special symbols except letters and digits from branch name and limit it to 15 symbols
        val branch = currentCheckoutBranch.ref.name
            .removePrefix(REFS_HEADS)
            .substring(0, 15)
            .replace("[^A-Za-z0-9]".toRegex(), "")

        RevWalk(repo).use { walk ->
            val commit: RevCommit = walk.parseCommit(headCommit)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
            dateFormat.timeZone = TimeZone.getDefault()
            val formattedDate = dateFormat.format(Date(commit.commitTime * 1000L))

            return "$formattedDate.$branch.${commit.name.substring(0, 5)}"
        }
    }
}
