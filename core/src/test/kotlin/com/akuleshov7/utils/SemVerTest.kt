package com.akuleshov7.utils

import com.akuleshov7.vercraft.core.SemVer
import kotlin.test.Test
import kotlin.test.assertTrue

class SemVerTest {
    @Test
    fun checkingEquality() {
        var thisVer = SemVer("1.0.0")
        var thatVer = SemVer("1.0.0")
        assertTrue { thisVer == thatVer }

        thisVer = SemVer("2.0.0")
        thatVer = SemVer("1.0.0")
        assertTrue { thisVer != thatVer }

        thisVer = SemVer("2.0.0")
        thatVer = SemVer("1.0.0")
        assertTrue { thisVer > thatVer }

        thisVer = SemVer("2.1.0")
        thatVer = SemVer("2.0.0")
        assertTrue { thisVer > thatVer }

        thisVer = SemVer("2.1.1")
        thatVer = SemVer("2.1.0")
        assertTrue { thisVer > thatVer }
    }
}