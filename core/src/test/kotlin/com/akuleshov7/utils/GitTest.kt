package com.akuleshov7.utils

import com.akuleshov7.vercraft.core.DefaultConfig
import com.akuleshov7.vercraft.core.Releases
import org.eclipse.jgit.api.Git
import java.io.File
import java.nio.file.Paths
import kotlin.test.Test

class GitTest {
    @Test
    fun smokeTest() {
        Git.open(File("../")).use { git ->
            val releases = Releases(git, DefaultConfig)
            val resultedVer = releases.version.calc()
            println(">>++ VerCrafted: $resultedVer")
        }
    }
}