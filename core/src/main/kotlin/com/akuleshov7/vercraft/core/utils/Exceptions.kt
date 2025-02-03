package com.akuleshov7.vercraft.core.utils

public const val ERROR_PREFIX: String = "(!) ERROR:"

public const val WARN_PREFIX: String = "(!) WARN:"

public class ConnectionProblemException(host: String?): Exception("Git cannot connect to the remote repository: <$host>")