package com.akuleshov7.vercraft.tests.functional

import com.akuleshov7.vercraft.core.*
import com.akuleshov7.vercraft.utils.checkoutRef
import com.akuleshov7.vercraft.utils.vercraftTest
import org.eclipse.jgit.api.Git
import kotlin.test.Test
import kotlin.test.assertNotNull

class GitMakeNewRelease {
    @Test
    fun `make new release`() {
        deleteBranchAndTag("refs/heads/release/1.2.x", "v1.2.0")

        Git.open(vercraftTest).use { git ->
            checkoutRef(git, DETACHED_COMMIT_MAIN_AFTER_1_1_X_RELEASE)
        }

        makeRelease(
            vercraftTest,
            SemVerReleaseType.MINOR,
            Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, CheckoutBranch("main"))
        )

        Git.open(vercraftTest).use { git ->
            assertNotNull(git.branchList().call().map { it.name }.find { it == "refs/heads/release/1.2.x" })
        }

        deleteBranchAndTag("refs/heads/release/1.2.x", "v1.2.0")
    }

    fun deleteBranchAndTag(branch: String, tag: String) {
        Git.open(vercraftTest).use { git ->
            git.branchDelete()
                .setBranchNames(branch)
                .setForce(true)
                .call()

            git.tagDelete()
                .setTags(tag)
                .call();
        }
    }
}
