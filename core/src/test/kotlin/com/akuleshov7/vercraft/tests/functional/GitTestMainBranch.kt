package com.akuleshov7.vercraft.tests.functional

import com.akuleshov7.vercraft.core.CheckoutBranch
import com.akuleshov7.vercraft.core.Config
import com.akuleshov7.vercraft.core.DefaultConfig
import com.akuleshov7.vercraft.core.Releases
import com.akuleshov7.vercraft.utils.checkoutRef
import org.eclipse.jgit.api.Git
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

const val DETACHED_COMMIT_RIGHT_AFTER_RELEASE = "4b3a62cf1e783523d3de57691ef5bc4ac11d5c3c"
const val DETACHED_COMMIT_1_AFTER_RELEASE_MAIN = "9f4c9ba873ed8329a8b913df9bf31c7d81671d8b"
const val DETACHED_COMMIT_BETWEEN_RELEASES = "df22d05e681404c1ed98b0db0bf041d60236d14c"
const val DETACHED_COMMIT_WITHOUT_RELEASES = "84f4bf70c9ca4da3f6e253d5c206159838ab2522"


class GitTestMainBranch {
    @Test
    fun `detached commit from main right after the release but not the last in main`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            checkoutRef(git, DETACHED_COMMIT_RIGHT_AFTER_RELEASE)
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, CheckoutBranch("main")))
            val resultedVer = releases.version.calc()
            println(resultedVer)
            assertEquals("1.2.1-main+4b3a6", resultedVer.toString())
        }
    }

    @Test
    fun `detached commit last in main`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            checkoutRef(git, DETACHED_COMMIT_1_AFTER_RELEASE_MAIN)
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, CheckoutBranch("main")))
            val resultedVer = releases.version.calc()
            println(resultedVer)
            assertEquals("1.2.2-main+9f4c9", resultedVer.toString())
        }
    }

    @Test
    fun `just main was checked-out`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            checkoutRef(git, "main")
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, CheckoutBranch("main")))
            val resultedVer = releases.version.calc()
            println(resultedVer)
            assertEquals("1.2.2-main+9f4c9", resultedVer.toString())
        }
    }

    @Test
    fun `release 1 1 0 commit but on main`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            checkoutRef(git, RELEASE_1_1_0_COMMIT_MAIN)
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, CheckoutBranch("main")))
            val resultedVer = releases.version.calc()
            println(resultedVer)
            assertEquals("1.2.0-main+9e2e2", resultedVer.toString())
        }
    }

    @Test
    fun `commit between releases`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            checkoutRef(git, DETACHED_COMMIT_BETWEEN_RELEASES)
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, CheckoutBranch("main")))
            val resultedVer = releases.version.calc()
            println(resultedVer)
            assertEquals("0.2.5-main+df22d", resultedVer.toString())
        }
    }

    @Test
    fun `commit in main before any releases`() {
        Git.open(File("src/test/resources/vercraft-test")).use { git ->
            checkoutRef(git, DETACHED_COMMIT_WITHOUT_RELEASES)
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, CheckoutBranch("main")))
            val resultedVer = releases.version.calc()
            println(resultedVer)
            assertEquals("0.0.1-main+84f4b", resultedVer.toString())
        }
    }
}
