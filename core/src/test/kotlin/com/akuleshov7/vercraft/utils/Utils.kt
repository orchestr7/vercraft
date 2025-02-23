package com.akuleshov7.vercraft.utils

import org.eclipse.jgit.api.Git
import java.io.File

val vercraftTest = File("src/test/resources/vercraft-test")

fun checkoutRef (git: Git, ref: String) {
    git.checkout().setName(ref).call()
}
