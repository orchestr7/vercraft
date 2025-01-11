package com.akuleshov7.vercraft.core.utils

public const val ERROR_PREFIX: String = "(!) ERROR:"

public class ConnectionProblemException(host: String?): Exception("Git cannot connect to the remote repository: <$host>")