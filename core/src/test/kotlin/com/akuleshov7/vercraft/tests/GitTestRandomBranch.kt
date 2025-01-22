package com.akuleshov7.vercraft.tests

import com.akuleshov7.vercraft.core.Config
import com.akuleshov7.vercraft.core.DefaultConfig
import com.akuleshov7.vercraft.core.Releases
import com.akuleshov7.vercraft.utils.checkoutRef
import org.eclipse.jgit.api.Git
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

const val INITIAL_COMMIT_TEST_BRANCH = "650c9d787eb1a6b912467fa241b821fe70577cc3"
const val FIRST_COMMIT_TEST_BRANCH = "fea9e01c191f8c7497081dfa6c982888fe575a0f"
const val SECOND_COMMIT_TEST_BRANCH = "d5e2a6dc062079a78fa9535ed67c0c4973d2a9b6"
const val LAST_COMMIT_TEST_BRANCH = "70982520bb947ab4331d3bf25dd73074cad7e8a5"


class GitTestRandomBranch {
    @Test
    fun `initial commit test branch`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            checkoutRef(git, INITIAL_COMMIT_TEST_BRANCH)
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, "feature/test"))
            val resultedVer = releases.version.calc()
            println(resultedVer)
            assertEquals("2025-01-22-test-0-650c9", resultedVer.toString())
        }
    }

    @Test
    fun `first commit test branch`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            checkoutRef(git, FIRST_COMMIT_TEST_BRANCH)
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, "feature/test"))
            val resultedVer = releases.version.calc()
            println(resultedVer)
            assertEquals("2025-01-22-test-1-fea9e", resultedVer.toString())
        }
    }

    @Test
    fun `second commit test branch`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            checkoutRef(git, SECOND_COMMIT_TEST_BRANCH)
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, "feature/test"))
            val resultedVer = releases.version.calc()
            println(resultedVer)
            assertEquals("2025-01-22-test-2-d5e2a", resultedVer.toString())
        }
    }

    @Test
    fun `last commit test branch`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            checkoutRef(git, LAST_COMMIT_TEST_BRANCH)
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, "feature/test"))
            val resultedVer = releases.version.calc()
            println(resultedVer)
            assertEquals("2025-01-22-test-4-70982", resultedVer.toString())
        }
    }

    @Test
    fun `just test branch`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            if (git.repository.findRef("feature/test") == null) {
                git.checkout()
                    .setCreateBranch(true)
                    .setName("feature/test") // Local branch name
                    .setStartPoint("${DefaultConfig.remote}/feature/test") // Remote branch
                    .call()
            } else {
                git.checkout()
                    .setName("feature/test")
                    .call()
            }
            checkoutRef(git, "feature/test")
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, null))
            val resultedVer = releases.version.calc()
            println(resultedVer)
            assertEquals("2025-01-22-test-4-70982", resultedVer.toString())

            git.checkout()
                .setName(DefaultConfig.defaultMainBranch)
                .call()

            git.branchDelete()
                .setBranchNames("feature/test")
                .setForce(true)
                .call()
        }
    }
}
