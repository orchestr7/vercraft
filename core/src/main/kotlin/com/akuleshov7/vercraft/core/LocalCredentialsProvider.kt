package com.akuleshov7.vercraft.core

import org.eclipse.jgit.transport.CredentialItem
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish

public object LocalCredentialsProvider : CredentialsProvider() {
    override fun isInteractive(): Boolean = false

    override fun supports(vararg items: CredentialItem?): Boolean = true

    override fun get(uri: URIish?, vararg items: CredentialItem?): Boolean {
        // Let JGit use system credentials (e.g., SSH agent or ~/.git-credentials)
        return true
    }
}