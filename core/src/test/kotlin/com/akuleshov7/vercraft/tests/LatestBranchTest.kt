package com.akuleshov7.vercraft.tests

import com.akuleshov7.vercraft.core.CheckoutBranch
import com.akuleshov7.vercraft.core.Config
import com.akuleshov7.vercraft.core.DefaultConfig
import com.akuleshov7.vercraft.core.Releases
import com.akuleshov7.vercraft.utils.checkoutRef
import com.akuleshov7.vercraft.utils.vercraftTest
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevWalk
import kotlin.test.Test
import kotlin.test.assertEquals

const val DETACHED_COMMIT_RIGHT_AFTER_RELEASE_IN_THE_MIDDLE = "df22d05e681404c1ed98b0db0bf041d60236d14c"
const val DETACHED_COMMIT_AT_1_1_0 = "9e2e23d82db76a5b6f1aee8a2bb8ffaf485ee9a0"
const val DETACHED_COMMIT_WITHOUT_RELEASE_BEFORE_IT = "84f4bf70c9ca4da3f6e253d5c206159838ab2522"

class LatestBranchTest {
    @Test
    fun `trying to find latest branch for a git log`() {
        Git.open(vercraftTest).use { git ->
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
        Git.open(vercraftTest).use { git ->
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
        Git.open(vercraftTest).use { git ->
            checkoutRef(git, DETACHED_COMMIT_WITHOUT_RELEASE_BEFORE_IT)
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, CheckoutBranch("main")))
            val commit = RevWalk(releases.repo).use {
                it.parseCommit(ObjectId.fromString(DETACHED_COMMIT_WITHOUT_RELEASE_BEFORE_IT))
            }
            val res = releases.getLatestReleaseForCommit(commit, releases.defaultMainBranch)?.ref?.name
            println(res)
            assertEquals(null, res)
        }
    }
}