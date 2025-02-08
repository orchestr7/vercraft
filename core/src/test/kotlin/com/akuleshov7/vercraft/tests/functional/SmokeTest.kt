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

// NB: Never run this test without a checked-out branch (detached HEAD) locally. On CI it will work due to env variables
class SmokeTest {
    @Test
    fun `smoke test`() {
        Git.open(File("../")).use { git ->
            val releases = Releases(git, Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote))
            val resultedVer = releases.version.calc()
            println(resultedVer)
        }
    }
}
