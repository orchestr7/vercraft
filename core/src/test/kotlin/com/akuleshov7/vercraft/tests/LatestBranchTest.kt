package com.akuleshov7.vercraft.tests

import com.akuleshov7.vercraft.core.CheckoutBranch
import com.akuleshov7.vercraft.core.Config
import com.akuleshov7.vercraft.core.DefaultConfig
import com.akuleshov7.vercraft.core.Releases
import com.akuleshov7.vercraft.utils.checkoutRef
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevWalk
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

const val DETACHED_COMMIT_RIGHT_AFTER_RELEASE_IN_THE_MIDDLE = "df22d05e681404c1ed98b0db0bf041d60236d14c"
const val DETACHED_COMMIT_AT_1_1_0 = "9f4c9ba873ed8329a8b913df9bf31c7d81671d8b"
const val DETACHED_COMMIT_WITHOUT_RELEASE_BEFORE_IT = "84f4bf70c9ca4da3f6e253d5c206159838ab2522"

class LatestBranchTest {
    @Test
    fun `trying to find latest branch for a git log`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            checkoutRef(git, DETACHED_COMMIT_RIGHT_AFTER_RELEASE_IN_THE_MIDDLE)
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, CheckoutBranch("main")))
            val commit = RevWalk(releases.repo).use {
                it.parseCommit(ObjectId.fromString(DETACHED_COMMIT_RIGHT_AFTER_RELEASE_IN_THE_MIDDLE))
            }
            val res = releases.getLatestReleaseForCommit(commit, releases.defaultMainBranch)?.ref?.name
            println(res)
            assertEquals("refs/remotes/origin/release/0.1.0", res)
        }
    }

    @Test
    fun `trying to find latest branch for a git log while we are at release commit`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            checkoutRef(git, DETACHED_COMMIT_AT_1_1_0)
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, CheckoutBranch("main")))
            val commit = RevWalk(releases.repo).use {
                it.parseCommit(ObjectId.fromString(DETACHED_COMMIT_AT_1_1_0))
            }
            val res = releases.getLatestReleaseForCommit(commit, releases.defaultMainBranch)?.ref?.name
            println(res)
            assertEquals("refs/remotes/origin/release/1.1.0", res)
        }
    }

    @Test
    fun `trying to find latest branch for a commit when there were no release branches`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            checkoutRef(git, DETACHED_COMMIT_WITHOUT_RELEASE_BEFORE_IT)
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, CheckoutBranch("main")))
            val commit = RevWalk(releases.repo).use {
                it.parseCommit(ObjectId.fromString(DETACHED_COMMIT_WITHOUT_RELEASE_BEFORE_IT))
            }
            val res = releases.getLatestReleaseForCommit(commit, releases.defaultMainBranch)?.ref?.name
            println(res)
            assertEquals("refs/remotes/origin/release/1.1.0", res)
        }
    }
}