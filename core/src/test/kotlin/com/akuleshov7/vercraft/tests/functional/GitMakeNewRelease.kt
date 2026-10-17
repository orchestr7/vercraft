package com.akuleshov7.vercraft.tests.functional

import com.akuleshov7.vercraft.core.CheckoutBranch
import com.akuleshov7.vercraft.core.Config
import com.akuleshov7.vercraft.core.DefaultConfig
import com.akuleshov7.vercraft.core.DefaultMainBranch
import com.akuleshov7.vercraft.core.Remote
import com.akuleshov7.vercraft.core.SemVerReleaseType
import com.akuleshov7.vercraft.core.makeRelease
import com.akuleshov7.vercraft.utils.checkoutRef
import com.akuleshov7.vercraft.utils.vercraftTest
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import java.io.File
import java.util.UUID
import kotlin.io.path.createTempDirectory
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

    @Test
    fun `patch release after a previous patch returns the version of the tag it creates`() {
        val version = makeRelease(releaseLineWithPatch(), SemVerReleaseType.PATCH, mainConfig())
        assertEquals("2.9.2", version.toString(), "PATCH release returned a version other than the tag it created")
    }

    @Test
    fun `patch release without release branches returns the name of the tag it creates`() {
        val repo = mainWithoutReleases()
        val version = makeRelease(repo, SemVerReleaseType.PATCH, mainConfig())
        val tags = Git.open(repo).use { git -> git.tagList().call().map { Repository.shortenRefName(it.name) } }
        assertEquals(tags, listOf("v$version"), "PATCH release returned a version that does not name the created tag")
    }

    private fun mainConfig() = Config(DefaultMainBranch("main"), Remote("origin"), CheckoutBranch("main"))

    private fun mainWithoutReleases(): File {
        val dir = createTempDirectory("vercraft-main").toFile()
        Git.init().setDirectory(dir).setInitialBranch("main").call().use { git -> repeat(3) { commit(git) } }
        return dir
    }

    private fun releaseLineWithPatch(): File {
        val dir = createTempDirectory("vercraft-release-line").toFile()
        Git.init().setDirectory(dir).setInitialBranch("main").call().use { git ->
            commit(git)
            git.branchCreate().setName("release/2.9.x").call()
            git.tag().setName("v2.9.0").call()
            commit(git)
            git.checkout().setName("release/2.9.x").call()
            commit(git)
            git.tag().setName("v2.9.1").call()
            commit(git)
            git.checkout().setName("main").call()
        }
        return dir
    }

    private fun commit(git: Git) {
        val name = "${UUID.randomUUID()}.txt"
        File(git.repository.workTree, name).writeText(UUID.randomUUID().toString())
        git.add().addFilepattern(name).call()
        git.commit().setMessage(name).call()
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