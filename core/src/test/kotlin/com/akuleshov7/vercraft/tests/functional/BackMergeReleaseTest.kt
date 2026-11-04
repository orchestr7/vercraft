package com.akuleshov7.vercraft.tests.functional

import com.akuleshov7.vercraft.core.CheckoutBranch
import com.akuleshov7.vercraft.core.Config
import com.akuleshov7.vercraft.core.DefaultConfig
import com.akuleshov7.vercraft.core.Releases
import org.eclipse.jgit.api.Git
import java.io.File
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression tests for the scenario where a release branch is merged back into the main branch.
 *
 * When the merge happens, every commit on the release branch becomes reachable from main, which
 * shifts the merge-base forward. A pure commit-distance-from-main calculation will then report a
 * patch number that has already been released, blocking subsequent publish steps.
 *
 * These tests pin the expected behaviour: existing `v<major>.<minor>.<patch>` tags act as the
 * authoritative anchor for the patch counter, regardless of where the merge-base ends up.
 */
class BackMergeReleaseTest {

    @Test
    fun `release branch merged back into main computes next patch from latest tag`() {
        withTempGitRepo { git ->
            // master: M0 (init)
            commit(git, "init")
            tag(git, "anchor")

            // create release/0.2.x from master
            git.branchCreate().setName("release/0.2.x").call()
            git.checkout().setName("release/0.2.x").call()

            // first patch commit on release branch, tagged v0.2.1
            commit(git, "fix on release branch")
            tag(git, "v0.2.1")

            val firstPatchCommit = git.repository.resolve("HEAD").name

            // back-merge release/0.2.x into master
            git.checkout().setName("main").call()
            git.merge()
                .include(git.repository.resolve("release/0.2.x"))
                .setMessage("Merge release/0.2.x")
                .call()

            // additional fix on release branch after the back-merge
            git.checkout().setName("release/0.2.x").call()
            commit(git, "another fix on release branch")

            val releases = Releases(
                git,
                Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, CheckoutBranch("release/0.2.x"))
            )
            val resultedVer = releases.version.calc()

            // Expected: v0.2.1 already exists at firstPatchCommit, HEAD is 1 commit ahead → 0.2.2
            assertEquals("0.2.2", resultedVer.toString())

            // Sanity: the tag really sits on the first patch commit on the release branch
            val taggedObj = git.repository.refDatabase
                .peel(git.repository.findRef("refs/tags/v0.2.1"))
                .let { it.peeledObjectId ?: it.objectId }
            assertEquals(firstPatchCommit, taggedObj.name)
        }
    }

    @Test
    fun `release branch without any patch tags falls back to commit-distance calculation`() {
        withTempGitRepo { git ->
            // master: M0 (init)
            commit(git, "init")

            // create release/0.3.x from master (no tags yet)
            git.branchCreate().setName("release/0.3.x").call()
            git.checkout().setName("release/0.3.x").call()
            commit(git, "first patch on release branch")

            val releases = Releases(
                git,
                Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, CheckoutBranch("release/0.3.x"))
            )
            val resultedVer = releases.version.calc()

            // No tags → legacy behaviour: branch SemVer 0.3.0 + distance(1) = 0.3.1
            assertEquals("0.3.1", resultedVer.toString())
        }
    }

    private fun withTempGitRepo(block: (Git) -> Unit) {
        val dir = Files.createTempDirectory("vercraft-backmerge-").also { it.createDirectories() }.toFile()
        try {
            Git.init().setDirectory(dir).setInitialBranch("main").call().use { git ->
                git.repository.config.apply {
                    setString("user", null, "name", "vercraft-test")
                    setString("user", null, "email", "vercraft-test@example.com")
                    save()
                }
                block(git)
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun commit(git: Git, message: String) {
        val marker = File(git.repository.workTree, "marker-${System.nanoTime()}.txt")
        marker.writeText(message)
        git.add().addFilepattern(marker.name).call()
        git.commit().setMessage(message).setSign(false).call()
    }

    private fun tag(git: Git, name: String) {
        git.tag().setName(name).setMessage(name).call()
    }
}
