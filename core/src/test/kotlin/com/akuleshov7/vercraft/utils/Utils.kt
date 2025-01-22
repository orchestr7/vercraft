package com.akuleshov7.vercraft.utils

import org.eclipse.jgit.api.Git


fun checkoutRef (git: Git, ref: String) {
    git.checkout().setName(ref).call()
}