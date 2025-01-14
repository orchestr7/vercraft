package com.akuleshov7.vercraft.core

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit

public const val GITLAB_BRANCH_REF: String = "CI_COMMIT_REF_NAME"
public const val GITHUB_BRANCH_REF: String = "GITHUB_REF_NAME"
public const val BITBUCKET_BRANCH_REF: String = "BITBUCKET_BRANCH"
public const val VERCRAFT_BRANCH_REF: String = "VERCRAFT_BRANCH"

public class Branch(git: Git, public val ref: Ref) {
    public val gitLog: List<RevCommit> = git.log().add(ref.objectId).call().toList()

    /**
     * Counts the number of commits (from the end) which were made in branch after the [[startCommit]]. For example:
     * 1 -> 2 -> 3 -> 4 -> latest
     * + -> + -> v0 -> v1 -> v2
     * commitNumberAfterThis(3) == 2
     */
    public fun numberOfCommitsAfter(startCommit: RevCommit): Int {
        var count = 0
        // gitLog is always reverted from the last commit to the first one
        gitLog.forEach { commitInBranch ->
            if (commitInBranch.id.name != startCommit.id.name) {
                ++count
            } else {
                return count
            }
        }

        return -1
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
    public fun findBaseCommitIn(sourceBase: Branch): RevCommit? {
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
