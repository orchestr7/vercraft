package com.akuleshov7.vercraft.core

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

public class VersionCalculator(
    public val git: Git,
    private val config: Config,
    private val releases: Releases,
    private val currentCheckoutBranch: Branch,
) {
    private val repo: Repository = git.repository
    // some CI platforms create "fake" or "synthetic" merge commit for PR
    // (to check that final build works well, they use the result of source branch merged into target branch)
    // that's why calculating <HEAD> will sometimes be invalid (it will be a hash of synthetic merge commit)
    // HOWEVER! There is a great hack: we know hash codes of both parents: of source and target branches
    private val headCommit = RevWalk(repo).use {
        val realHeadOrMergeCommit = it.parseCommit(repo.resolve("HEAD"))
        // if it has two parents - then it is a merge commit
        // we also need to check that it is not present in the currentCheckoutBranch (meaning, that it is a synthetic commit)
        if (realHeadOrMergeCommit.parentCount > 1 && !currentCheckoutBranch.gitLog.contains(realHeadOrMergeCommit)) {
            // parent == 0 -> commit from target branch (usually main)
            // parent == 1 -> commit from source branch
            realHeadOrMergeCommit.getParent(1)
        } else {
            realHeadOrMergeCommit
        }
    }

    public fun calc(): SemVer =
        when {
            currentCheckoutBranch.ref!!.name
                .shortName(config.remote.value) == config.defaultMainBranch.value -> calcVersionInDefaultMain()
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
     * The version for commit yyy is derived by:
     * 1) incremented MINOR version of the last release version (0.1.0)
     * 2) incrementing PATCH version by the number of commits between commits bbb and yyy (in this case, 2 commits).
     * Therefore, the resulting version for (yyy) will be 0.2.1.
     *
     * (!) Additionally, to avoid the confusion with releases, the version will be appended with a postfix consisting of
     * "-main" and the first 5 characters of the commit hash. So the result will be as following:
     * aaa -> bbb (release/0.1.0) -> 0.2.0-main+hash -> 0.2.1-main+hash (?)
     */
    private fun calcVersionInDefaultMain(): SemVer {
        val latestRelease = releases.getLatestReleaseForCommit(headCommit, currentCheckoutBranch)
        // if no releases were made so far, then will calculate version starting from the initial commit
        val baseCommit = latestRelease
            ?.intersectionCommitWithBranch(currentCheckoutBranch)
            ?: currentCheckoutBranch.gitLog.last()

        val distance = currentCheckoutBranch.distanceBetweenCommits(baseCommit, headCommit)

        val shortedHashCode = headCommit.name.substring(0, 5)

        return latestRelease
            ?.let {
                latestRelease.version
                    .nextVersion(SemVerReleaseType.MINOR)
                    .incrementPatchVersion(distance)
                    .setPostFix("${config.defaultMainBranch}+$shortedHashCode")
            }
            ?: run { SemVer(0, 0, distance).setPostFix("${config.defaultMainBranch}+$shortedHashCode") }
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
     *
     * (!) When the release branch has already accumulated `v<major>.<minor>.<patch>` tags, those tags
     * are treated as the authoritative record of past patch releases and the version is computed as
     * `latest_tag_patch + commits_since_that_tag`. This avoids miscalculations when the release
     * branch has been merged back into main (which would otherwise shift the merge-base and break
     * pure commit counting).
     */
    private fun calcVersionInRelease(): SemVer {
        val branchVersion = releases.releaseBranches.find { it.ref == currentCheckoutBranch.ref }
            ?.version
            ?: throw IllegalStateException(
                "Cannot find branch ${currentCheckoutBranch.ref!!.name} in the list of release branches:" +
                        "${releases.releaseBranches}"
            )

        // Prefer existing patch tags when present: they are an authoritative record of what has
        // already been released, and using them avoids miscalculations when the release branch has
        // been merged back into main (which shifts the merge-base and breaks pure commit counting).
        // Falls back to the legacy distance-from-main behaviour when no matching tags exist yet,
        // preserving backwards compatibility for projects that have never tagged a patch release.
        val latestPatchTag = findLatestReachablePatchTag(branchVersion)
        if (latestPatchTag != null) {
            val (taggedVersion, taggedCommit) = latestPatchTag
            val sinceTag = currentCheckoutBranch.distanceBetweenCommits(taggedCommit, headCommit)
            return SemVer(taggedVersion.major, taggedVersion.minor, taggedVersion.patch + sinceTag)
        }

        val distance = distanceFromDefaultMainBranch()
        return branchVersion.incrementPatchVersion(distance)
    }

    /**
     * Find the highest-patch tag matching the current release branch (`v<major>.<minor>.<patch>`)
     * that is reachable from the current HEAD commit on this release branch.
     *
     * Both annotated and lightweight tags are supported. Tags pointing at commits which are not
     * part of the release branch history are ignored.
     */
    private fun findLatestReachablePatchTag(branchVersion: SemVer): Pair<SemVer, RevCommit>? {
        val tagPattern = Regex("^v?${branchVersion.major}\\.${branchVersion.minor}\\.(0|[1-9]\\d*)$")
        val branchCommits: Set<String> = currentCheckoutBranch.gitLog.map { it.id.name }.toSet()

        return git.tagList().call().mapNotNull { tagRef ->
            val shortName = tagRef.name.substringAfterLast("refs/tags/")
            val match = tagPattern.matchEntire(shortName) ?: return@mapNotNull null
            val patch = match.groupValues[1].toInt()
            val taggedCommit = resolveTaggedCommit(tagRef) ?: return@mapNotNull null
            if (taggedCommit.id.name !in branchCommits) return@mapNotNull null
            SemVer(branchVersion.major, branchVersion.minor, patch) to taggedCommit
        }.maxByOrNull { it.first.patch }
    }

    private fun resolveTaggedCommit(tagRef: Ref): RevCommit? {
        val peeled = repo.refDatabase.peel(tagRef)
        val objectId = peeled.peeledObjectId ?: tagRef.objectId ?: return null
        return RevWalk(repo).use { walk -> walk.parseCommit(objectId) }
    }

    /**
     * The [[currentCheckoutBranch]] refers to any non-release branch (e.g., "feature/blabla") in this context.
     *
     * To calculate the commit version, we determine the distance (number of commits) between the base commit
     * (where the branch diverged from the main branch) and the current commit. This distance is used as the PATCH version,
     * while the MAJOR and MINOR versions are set to 0 (resulting in a version format of 0.0.x).
     *
     * (!) Additionally, the version will include:
     * - The last segment of the branch name (substring after the final "/").
     * - The current date in "yyyy-MM-dd" format.
     *
     * For example, if the branch is "feature/blabla" and the commit distance is 1,
     * the version will be: "2024-12-24-blabla-1". With such version the clean-up of useless branch builds will be easier.
     */
    private fun calcVersionInBranch(): SemVer {
        // replace all special symbols except letters and digits from branch name and limit it to 10 symbols
        val branchName = currentCheckoutBranch.ref!!.name.substringAfterLast("/")
        val branch = branchName
            .substring(0, min(branchName.length, 10))
            .replace("[^A-Za-z0-9]".toRegex(), "")

        val distance = distanceFromDefaultMainBranch()

        RevWalk(repo).use { walk ->
            val commit: RevCommit = walk.parseCommit(headCommit)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val formattedDate = dateFormat.format(Date(commit.commitTime * 1000L))

            return SemVer(NO_MAJOR, NO_MINOR, distance)
                .setPrefix("$formattedDate-$branch")
                .setPostFix(commit.name.substring(0, 5))
        }
    }

    private fun distanceFromDefaultMainBranch(): Int {
        val baseCommit = currentCheckoutBranch.baseCommitInMain
            ?: throw IllegalStateException(
                "Can't find common ancestor commits between ${config.defaultMainBranch} " +
                        "and ${currentCheckoutBranch.ref!!.name} branches. Looks like these branches have no relation " +
                        "and that is an inconsistent git state."
            )

        return currentCheckoutBranch.distanceBetweenCommits(baseCommit, headCommit)
    }
}
