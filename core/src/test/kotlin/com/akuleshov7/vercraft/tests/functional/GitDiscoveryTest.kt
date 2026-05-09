package com.akuleshov7.vercraft.tests.functional

import com.akuleshov7.vercraft.core.CheckoutBranch
import com.akuleshov7.vercraft.core.Config
import com.akuleshov7.vercraft.core.DefaultConfig
import com.akuleshov7.vercraft.core.gitVersion
import com.akuleshov7.vercraft.utils.checkoutRef
import com.akuleshov7.vercraft.utils.vercraftTest
import org.eclipse.jgit.api.Git
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class GitDiscoveryTest {
    @Test
    fun `gitVersion works from subdirectory of git repository`() {
        val subDir = File(vercraftTest, "subdir-for-discovery-test").also { it.mkdir() }
        try {
            Git.open(vercraftTest).use { git ->
                checkoutRef(git, "main")
            }

            val config = Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, CheckoutBranch("main"))
            val version = gitVersion(subDir, config)
            assertTrue(
                version.matches(Regex("""\d+\.\d+\.\d+.*""")),
                "Expected semver-like version, got: '$version'"
            )
        } finally {
            subDir.delete()
        }
    }
}