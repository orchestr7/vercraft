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
const val FIRST_COMMIT_IN_RELEASE_1_1_0 = "77b67741ab1030ad4b476820a4835302022be416"
const val SECOND_COMMIT_IN_RELEASE_1_1_0 = "2b309a6616022a477a41da1c37a1f87d52a595e5"

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
    fun `first commit in release 1 1 0`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            checkoutRef(git, FIRST_COMMIT_IN_RELEASE_1_1_0)
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, "release/1.1.0"))
            val resultedVer = releases.version.calc()
            println(resultedVer)
            assertEquals("1.1.1", resultedVer.toString())
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
    fun `release branch`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            checkoutRef(git, "release/1.1.0")
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, "release/1.1.0"))
            val resultedVer = releases.version.calc()
            println(resultedVer)
            assertEquals("1.1.2", resultedVer.toString())
        }
    }
}
