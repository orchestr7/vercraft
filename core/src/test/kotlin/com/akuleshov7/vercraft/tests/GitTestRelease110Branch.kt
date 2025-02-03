package com.akuleshov7.vercraft.tests

import com.akuleshov7.vercraft.core.Config
import com.akuleshov7.vercraft.core.DefaultConfig
import com.akuleshov7.vercraft.core.Releases
import com.akuleshov7.vercraft.utils.checkoutRef
import org.eclipse.jgit.api.Git
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

const val RELEASE_1_1_0_COMMIT_MAIN = "9e2e23d82db76a5b6f1aee8a2bb8ffaf485ee9a0"
const val FIRST_COMMIT_IN_RELEASE_1_1_0 = "13b9e10f068ea1243dc67094d7fc09d7c05ede86"
const val SECOND_COMMIT_IN_RELEASE_1_1_0 = "1f22b4ced648c6b8b5f36dc2e900ffc7b39a2ccb"

class GitTestRelease110Branch {
    @Test
    fun `release 1 1 0 commit but on release branch`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            checkoutRef(git, RELEASE_1_1_0_COMMIT_MAIN)
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, "release/1.1.0"))
            val resultedVer = releases.version.calc()
            println(resultedVer)
            assertEquals("1.1.0", resultedVer.toString())
        }
    }

    @Test
    fun `first commit in release 1 1 0, no local branch`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            checkoutRef(git, FIRST_COMMIT_IN_RELEASE_1_1_0)
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, "release/1.1.0"))
            val resultedVer = releases.version.calc()
            println(resultedVer)
            assertEquals("1.1.1", resultedVer.toString())
        }
    }

    @Test
    fun `first commit in release 1 1 0 with checked-out local`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            if (git.repository.findRef("release/1.1.0") == null) {
                git.checkout()
                    .setCreateBranch(true)
                    .setName("release/1.1.0") // Local branch name
                    .setStartPoint("${DefaultConfig.remote}/release/1.1.0") // Remote branch
                    .call()
            } else {
                git.checkout()
                    .setName("release/1.1.0")
                    .call()
            }

            checkoutRef(git, FIRST_COMMIT_IN_RELEASE_1_1_0)
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, "release/1.1.0"))
            val resultedVer = releases.version.calc()
            println(resultedVer)
            assertEquals("1.1.1", resultedVer.toString())

            git.checkout()
                .setName(DefaultConfig.defaultMainBranch)
                .call()

            git.branchDelete()
                .setBranchNames("release/1.1.0")
                .setForce(true)
                .call()
        }
    }

    @Test
    fun `second commit in release 1 1 0`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            checkoutRef(git, SECOND_COMMIT_IN_RELEASE_1_1_0)
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, "release/1.1.0"))
            val resultedVer = releases.version.calc()
            println(resultedVer)
            assertEquals("1.1.2", resultedVer.toString())
        }
    }

    @Test
    fun `release branch checkout local`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            if (git.repository.findRef("release/1.1.0") == null) {
                git.checkout()
                    .setCreateBranch(true)
                    .setName("release/1.1.0") // Local branch name
                    .setStartPoint("${DefaultConfig.remote}/release/1.1.0") // Remote branch
                    .call()
            } else {
                git.checkout()
                    .setName("release/1.1.0")
                    .call()
            }

            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, null))
            val resultedVer = releases.version.calc()
            println(resultedVer)
            assertEquals("1.1.3", resultedVer.toString())

            git.checkout()
                .setName(DefaultConfig.defaultMainBranch)
                .call()

            git.branchDelete()
                .setBranchNames("release/1.1.0")
                .setForce(true)
                .call()
        }
    }
}
