package com.akuleshov7.vercraft.tests.functional

import com.akuleshov7.vercraft.core.CheckoutBranch
import com.akuleshov7.vercraft.core.Config
import com.akuleshov7.vercraft.core.DefaultConfig
import com.akuleshov7.vercraft.core.SemVerReleaseType
import com.akuleshov7.vercraft.core.makeRelease
import com.akuleshov7.vercraft.utils.checkoutRef
import com.akuleshov7.vercraft.utils.vercraftTest
import org.eclipse.jgit.api.Git
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val SUBMODULE_BASE_HEAD = "9e2e23d82db76a5b6f1aee8a2bb8ffaf485ee9a0"

class GitMakeNewRelease {
    private val config = Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, CheckoutBranch("main"))

    @Test
    fun `make new release`() {
        Git.open(vercraftTest).use { git ->
            checkoutRef(git, DETACHED_COMMIT_BETWEEN_RELEASES)
        }

        val version = makeRelease(vercraftTest, SemVerReleaseType.MINOR, config)
        val shortBranch = "release/${version.semVerForBranch()}"
        val tagName = "v${version.justSemVer()}"

        try {
            Git.open(vercraftTest).use { git ->
                assertNotNull(
                    git.branchList().call().find { it.name == "refs/heads/$shortBranch" },
                    "Release branch $shortBranch should be created for MINOR release"
                )
            }
        } finally {
            deleteBranchAndTagIfExist(shortBranch, tagName)
            Git.open(vercraftTest).use { git -> checkoutRef(git, SUBMODULE_BASE_HEAD) }
        }
    }

    @Test
    fun `patch release should not create a new branch`() {
        Git.open(vercraftTest).use { git -> checkoutRef(git, DETACHED_COMMIT_BETWEEN_RELEASES) }
        val minorVersion = makeRelease(vercraftTest, SemVerReleaseType.MINOR, config)
        val releaseBranch = "release/${minorVersion.semVerForBranch()}"
        val minorTag = "v${minorVersion.justSemVer()}"

        val branchesBefore = Git.open(vercraftTest).use { git ->
            git.checkout().setName(releaseBranch).call()
            File(vercraftTest, "patch-fix.txt").writeText("bugfix")
            git.add().addFilepattern("patch-fix.txt").call()
            git.commit().setMessage("patch fix commit").call()
            checkoutRef(git, DETACHED_COMMIT_RIGHT_AFTER_RELEASE)
            git.branchList().call().map { it.name }.filter { it.startsWith("refs/heads/") }.toSet()
        }

        var patchTag: String? = null
        try {
            val patchVersion = makeRelease(vercraftTest, SemVerReleaseType.PATCH, config)
            patchTag = "v${patchVersion.justSemVer()}"

            Git.open(vercraftTest).use { git ->
                val branchesAfter = git.branchList().call().map { it.name }
                    .filter { it.startsWith("refs/heads/") }.toSet()
                assertEquals(branchesBefore, branchesAfter, "PATCH release should not create any new branches")
                assertNotNull(
                    git.tagList().call().find { it.name == "refs/tags/$patchTag" },
                    "Tag $patchTag should be created for PATCH release"
                )
            }
        } finally {
            Git.open(vercraftTest).use { git -> checkoutRef(git, SUBMODULE_BASE_HEAD) }
            patchTag?.let { deleteBranchAndTagIfExist(null, it) }
            deleteBranchAndTagIfExist(releaseBranch, minorTag)
            File(vercraftTest, "patch-fix.txt").delete()
        }
    }

    private fun deleteBranchAndTagIfExist(branch: String?, tag: String) {
        Git.open(vercraftTest).use { git ->
            if (branch != null) {
                try {
                    git.branchDelete().setBranchNames(branch).setForce(true).call()
                } catch (_: Exception) {
                }
            }
            try {
                git.tagDelete().setTags(tag).call()
            } catch (_: Exception) {
            }
        }
    }
}