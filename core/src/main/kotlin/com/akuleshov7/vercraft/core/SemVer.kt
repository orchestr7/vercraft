package com.akuleshov7.vercraft.core

import com.akuleshov7.vercraft.core.SemVerReleaseType.*
import com.akuleshov7.vercraft.core.utils.ERROR_PREFIX
import org.apache.logging.log4j.LogManager

internal const val NO_MAJOR = -1
internal const val NO_MINOR = -1

private val logger = LogManager.getLogger()

public enum class SemVerReleaseType {
    MAJOR,
    MINOR,
    PATCH
    ;

    public companion object {
        public fun fromValue(value: String): SemVerReleaseType =
            runCatching { SemVerReleaseType.valueOf(value.uppercase()) }
                .getOrNull()
                ?: run {
                    logger.error(
                        "$ERROR_PREFIX value [$value] is not allowed as type of SemVer release. " +
                                "Eligible values are: MAJOR, MINOR, PATCH"
                    )
                    throw IllegalArgumentException("Value [$value] is not allowed as type of SemVer release")
                }
    }
}

/**
 * Just a simple data class containing a version in a semver format parsed from string.
 * The only reason why it is not a data class, because I wanted a parsing logic in secondary constructor.
 */
public class SemVer : Comparable<SemVer> {
    public val major: Int
    public val minor: Int
    public val patch: Int
    public var prefix: String = ""
    private var postfix: String = ""

    public constructor(ver: String) {
        val parts = ver.removeReleasePrefix().split(".").map { it.toInt() }
        require(parts.size == 3) {
            throw IllegalArgumentException("SemVer version [$this] must be in the following format: 'major.minor.patch'")
        }
        major = parts[0]
        minor = parts[1]
        patch = parts[2]
    }

    public constructor(major: Int, minor: Int, patch: Int) {
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
        val major = if (this.major == NO_MAJOR) "" else "${this.major}."
        val minor = if (this.minor == NO_MINOR) "" else "${this.minor}."

        return (if (prefix != "") "$prefix-" else "") +
                "$major$minor${this.patch}" +
                (if (postfix != "") "-$postfix" else "")
    }

    public fun nextVersion(nextVersion: SemVerReleaseType): SemVer = when (nextVersion) {
        MAJOR -> SemVer(major + 1, 0, 0)
        MINOR -> SemVer(major, minor + 1, 0)
        PATCH -> SemVer(major, minor, patch + 1)
    }

    public fun incrementPatchVersion(increment: Int): SemVer = SemVer(major, minor, patch + increment)

    public fun setPostFix(postfix: String): SemVer {
        this.postfix = postfix
        return this
    }

    public fun setPrefix(prefix: String): SemVer {
        this.prefix = prefix
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
    val semVerRegex = Regex("""^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$""")
    return semVerRegex.matches(this)
}
