package com.akuleshov7.vercraft.core

import org.eclipse.jgit.lib.Constants

/**
 * Configuration class for all logic in VerCraft,
 * usually is passed to VerCraft with plugin
 */
public data class Config(
    val defaultMainBranch: DefaultMainBranch = DefaultMainBranch("main"),
    val remote: Remote = Remote(Constants.DEFAULT_REMOTE_NAME),
    val checkoutBranch: CheckoutBranch? = null,
)

@JvmInline
public value class DefaultMainBranch(public val value: String) {
    override fun toString(): String {
        return value
    }
}

@JvmInline
public value class Remote(public val value: String) {
    override fun toString(): String {
        return value
    }
}

@JvmInline
public value class CheckoutBranch(public val value: String) {
    override fun toString(): String {
        return value
    }
}

public val DefaultConfig: Config = Config()
