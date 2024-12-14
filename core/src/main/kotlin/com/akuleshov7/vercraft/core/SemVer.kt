package com.akuleshov7.vercraft.core

import com.akuleshov7.vercraft.core.SemVerReleaseType.*


public enum class SemVerReleaseType {
    MAJOR,
    MINOR,
    PATCH
}

/**
 * Just a simple data class containing a version in a semver format parsed from string.
 * The only reason why it is not a data class, because I wanted a parsing logic in secondary constructor.
 */
public class SemVer : Comparable<SemVer> {
    public val major: Long
    public val minor: Long
    public val patch: Long
    public var postfix: String = ""

    public constructor(ver: String) {
        val parts = ver.split(".").map { it.toLong() }
        require(parts.size == 3) {
            throw IllegalArgumentException("SemVer version [$this] must be in the following format: 'major.minor.patch'")
        }
        major = parts[0]
        minor = parts[1]
        patch = parts[2]
    }

    public constructor(major: Long, minor: Long, patch: Long) {
        this.major = major
        this.minor = minor
        this.patch = patch

    }

    public override operator fun compareTo(other: SemVer): Int {
        return when {
            // comparing major version first
            this.major > other.major -> 1
            this.major < other.major -> -1
            // if major is the same - then minor
            this.minor > other.minor -> 1
            this.minor < other.minor -> -1
            // if minor is the same - then minor
            this.patch > other.patch -> 1
            this.patch < other.patch -> -1

            else -> 0
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SemVer

        if (major != other.major) return false
        if (minor != other.minor) return false
        if (patch != other.patch) return false

        return true
    }

    override fun toString(): String {
        return "$major.$minor.$patch${if (postfix != "") "-$postfix" else ""}"
    }

    public fun nextVersion(nextVersion: SemVerReleaseType): SemVer = when (nextVersion) {
        MAJOR -> SemVer(major + 1, 0, 0)
        MINOR -> SemVer(major, minor + 1, 0)
        PATCH -> SemVer(major, minor, patch + 1)
    }

    public fun incrementPatchVersion(increment: Long): SemVer = SemVer(major, minor, patch + increment)

    public fun setPostFix(postfix: String): SemVer {
        this.postfix = postfix
        return this
    }

    override fun hashCode(): Int {
        var result = major.hashCode()
        result = 31 * result + minor.hashCode()
        result = 31 * result + patch.hashCode()
        return result
    }
}

public fun String.isValidSemVerFormat(): Boolean {
    try {
        SemVer(this)
    } catch (ex: Exception) {
        when (ex) {
            // only those exceptions are known to be thrown in case of invalid parsing in `parseSemVer`
            is NumberFormatException, is IllegalArgumentException -> return false
            else -> throw ex
        }
    }
    return true
}
