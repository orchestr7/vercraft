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

        for (commitInBranch in gitLog) {
            // check for the endCommit first since the log is reversed
            if (commitInBranch.id.name == endCommit.id.name) {
                endFound = true
            }

            if (commitInBranch.id.name == startCommit.id.name) {
                if (!endFound) {
                    throw IllegalStateException(
                        "Invalid commit order or git state: not able to find commit '${endCommit.name}'" +
                                "before the expected commit '${startCommit.name}'. Usually this happens on CI. " +
                                "Please try to checkout commit ref: <${endCommit.name} and run `./gradlew gitVersion` locally.>"
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
     *
     *  (!) IMPORTANT: the real problem with that approach is not to be confused by MERGE commits, which are present in
     *  both branches: original (main) and sub-branch. That's we you always need to track LAST seen common commit.
     *  And the only way to do it is to go from the initial commit in main to the latest commit.
     */
    public fun intersectionCommitWithBranch(sourceBase: Branch): RevCommit? {
        var previousCommit: RevCommit? = null

        // iterate from oldest to newest in sourceBase branch (main)
        for (commitInSub in this.gitLog.reversed()) {
            // find the first commit that is not present in main branch, but is there in sub-branch
            if (!sourceBase.gitLog.contains(commitInSub)) {
                // return the previous commit, before the first missing commit
                return previousCommit
            } else {
                previousCommit = commitInSub
            }
        }

        // no intersection found (should not happen in a properly branched repo)
        // so the only case when it can happen - when branches are equal (first creation)
        return previousCommit
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
