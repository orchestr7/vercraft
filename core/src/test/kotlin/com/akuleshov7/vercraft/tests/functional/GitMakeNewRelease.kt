package com.akuleshov7.vercraft.tests.functional

import com.akuleshov7.vercraft.core.CheckoutBranch
import com.akuleshov7.vercraft.core.Config
import com.akuleshov7.vercraft.core.DefaultConfig
import com.akuleshov7.vercraft.core.SemVerReleaseType
import com.akuleshov7.vercraft.core.makeRelease
import com.akuleshov7.vercraft.utils.checkoutRef
import com.akuleshov7.vercraft.utils.vercraftTest
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand.ResetType
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GitMakeNewRelease {

    @Test
    fun `make new release`() {
        deleteBranchAndTagIfExist("refs/heads/release/0.2.x", "v0.2.0")

        Git.open(vercraftTest).use { git ->
            checkoutRef(git, DETACHED_COMMIT_BETWEEN_RELEASES)
        }

        makeRelease(
            vercraftTest,
            SemVerReleaseType.MINOR,
            Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, CheckoutBranch("main"))
        )

        Git.open(vercraftTest).use { git ->
            assertNotNull(git.branchList().call().map { it.name }.find { it == "refs/heads/release/0.2.x" })
        }

        deleteBranchAndTag("refs/heads/release/0.2.x", "v0.2.0")
    }

    @Test
    fun `patch release should not create a new branch`() {
        Git.open(vercraftTest).use { git ->
            checkoutRef(git, DETACHED_COMMIT_BETWEEN_RELEASES)
        }

        Git.open(vercraftTest).use { git ->
            git.checkout().setName("release/0.1.x").call()
            File(vercraftTest, "patch-fix.txt").writeText("bugfix")
            git.add().addFilepattern("patch-fix.txt").call()
            git.commit().setMessage("patch fix commit").call()
        }

        Git.open(vercraftTest).use { git ->
            checkoutRef(git, DETACHED_COMMIT_BETWEEN_RELEASES)
        }

        val branchesBefore = Git.open(vercraftTest).use { git ->
            git.branchList().call().map { it.name }.filter { it.startsWith("refs/heads/") }.toSet()
        }

        try {
            val patchVersion = makeRelease(
                vercraftTest,
                SemVerReleaseType.PATCH,
                Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, CheckoutBranch("main"))
            )

            Git.open(vercraftTest).use { git ->
                val branchesAfter =
                    git.branchList().call().map { it.name }.filter { it.startsWith("refs/heads/") }.toSet()
                assertEquals(branchesBefore, branchesAfter, "PATCH release should not create any new branches")

                assertNotNull(
                    git.tagList().call().find { it.name == "refs/tags/v${patchVersion.justSemVer()}" },
                    "Tag v${patchVersion.justSemVer()} should be created for PATCH release"
                )
            }

            deleteTag("v${patchVersion.justSemVer()}")
        } finally {
            Git.open(vercraftTest).use { git ->
                git.checkout().setName("release/0.1.x").call()
                git.reset().setMode(ResetType.HARD).setRef("HEAD~1").call()
                File(vercraftTest, "patch-fix.txt").delete()
            }
        }
    }

    fun deleteBranchAndTag(branch: String, tag: String) {
        Git.open(vercraftTest).use { git ->
            git.branchDelete()
                .setBranchNames(branch)
                .setForce(true)
                .call()

            git.tagDelete()
                .setTags(tag)
                .call()
        }
    }

    private fun deleteBranchAndTagIfExist(branch: String, tag: String) {
        Git.open(vercraftTest).use { git ->
            try {
                git.branchDelete().setBranchNames(branch).setForce(true).call()
            } catch (_: Exception) {
            }
            try {
                git.tagDelete().setTags(tag).call()
            } catch (_: Exception) {
            }
        }
    }

    private fun deleteTag(tag: String) {
        Git.open(vercraftTest).use { git ->
            try {
                git.tagDelete().setTags(tag).call()
            } catch (_: Exception) {
            }
        }
    }
}