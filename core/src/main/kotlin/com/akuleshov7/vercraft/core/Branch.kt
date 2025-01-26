package com.akuleshov7.vercraft.core

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit

public const val GITLAB_BRANCH_REF: String = "CI_COMMIT_REF_NAME"
public const val GITHUB_HEAD_REF: String = "GITHUB_HEAD_REF"
public const val BITBUCKET_BRANCH: String = "BITBUCKET_BRANCH"
public const val VERCRAFT_BRANCH: String = "VERCRAFT_BRANCH"

public class Branch(git: Git, public val ref: Ref) {
    public val gitLog: List<RevCommit> = git.log().add(ref.objectId).call().toList()


    /**
     * Calculates the number of commits between two commits in a branch.
     * The gitLog is expected to be in reverse order (from latest to oldest).
     *
     * @param startCommit The starting commit.
     * @param endCommit The ending commit.
     * @return The number of commits between startCommit and endCommit, or -1 if:
     *         - Either commit is not found.
     *         - startCommit appears after endCommit in the reversed log (latest -> oldest).
     */
    public fun distanceBetweenCommits(startCommit: RevCommit, endCommit: RevCommit): Int {
        var count = 0
        var endFound = false

        gitLog.forEach {
            println(it.name + " ___ " + it.shortMessage)
        }

        println("BR:::::: ${ref.name}")

        for (commitInBranch in gitLog) {
            // check for the endCommit first since the log is reversed
            if (commitInBranch.id.name == endCommit.id.name) {
                endFound = true
            }

            if (commitInBranch.id.name == startCommit.id.name) {
                if (!endFound) {
                    throw IllegalStateException(
                        "Invalid commit order: Head commit '${endCommit.name.toString().substring(0, 5)}' was found " +
                                "before the expected starting commit '${startCommit.name.toString().substring(0, 5)}'. "
                    )
                }
                return count
            }

            if (endFound) {
                ++count
            }
        }

        // If we complete the loop without finding both commits, return -1
        throw IllegalArgumentException(
            "Not able to find neither commit ${startCommit.name.toString().substring(0, 5)}, " +
                    "nor commit ${endCommit.name.toString().substring(0, 5)}"
        )
    }


    /**
     * Finds the commit in [[sourceBase]] branch when a new sub-branch was created from it. For example:
     * main: 1 -> 2 -> 3 -> 4
     * sub:       |
     *            5
     *            |
     *            6
     *
     *  The base commit is 2.
     */
    public fun intersectionCommitWithBranch(sourceBase: Branch): RevCommit? {
        // (!) gitLog is always in a reversed order (from last to first commit):
        // 4 3 2 1
        // 6 5 2
        // ^ 6 not found
        //   ^ 5 not found
        //     ^ 2 - is a common commit
        this.gitLog.forEach { commitInSub ->
            val thisCommitInMainBranch = sourceBase.gitLog.firstOrNull { commitInSub.id.name == it.id.name }
            if (thisCommitInMainBranch != null) {
                return thisCommitInMainBranch
            }
        }

        return null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Branch

        if (ref != other.ref) return false
        if (gitLog != other.gitLog) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ref.hashCode()
        result = 31 * result + gitLog.hashCode()
        return result
    }
}
