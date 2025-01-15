package com.akuleshov7.vercraft.core

import org.eclipse.jgit.lib.Constants

/**
 * Configuration class for all logic in VerCraft,
 * usually is passed to VerCraft with plugin
 */
public data class Config(
    val defaultMainBranch: String = "main",
    val remote: String = Constants.DEFAULT_REMOTE_NAME,
    val checkoutBranch: String? = null,
)

public val DefaultConfig: Config = Config()
