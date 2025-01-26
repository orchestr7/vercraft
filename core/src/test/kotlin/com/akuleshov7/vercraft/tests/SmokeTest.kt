package com.akuleshov7.vercraft.tests

import com.akuleshov7.vercraft.core.Config
import com.akuleshov7.vercraft.core.DefaultConfig
import com.akuleshov7.vercraft.core.Releases
import com.akuleshov7.vercraft.core.gitVersion
import com.akuleshov7.vercraft.utils.checkoutRef
import org.eclipse.jgit.api.Git
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class SmokeTest {
    @Test
    fun `smoke test`() {
        gitVersion(File("../"), Config(DefaultConfig.defaultMainBranch, DefaultConfig.remote, "feature/readme"))
    }
}
