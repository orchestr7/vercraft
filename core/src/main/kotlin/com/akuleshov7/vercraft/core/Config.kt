package com.akuleshov7.vercraft.core

import org.eclipse.jgit.lib.Constants

/**
 * Configuration class for all logic in VerCraft,
 * usually is passed to VerCraft with plugin
 */
// TODO: migrate to value classes
public data class Config(
    val defaultMainBranch: String = "main",
    val remote: String = Constants.DEFAULT_REMOTE_NAME,
    val checkoutBranch: String? = null,
)

@JvmInline
public value class DefaultMainBranch(public val defaultMainBranch: String)

@JvmInline
public value class Remote(public val remote: String)

@JvmInline
public value class CheckoutBranch(public val checkoutBranch: String)

public val DefaultConfig: Config = Config()
