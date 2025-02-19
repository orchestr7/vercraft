package com.akuleshov7.vercraft

import com.akuleshov7.vercraft.core.Config
import com.akuleshov7.vercraft.core.SemVer
import com.akuleshov7.vercraft.core.SemVerReleaseType
import com.akuleshov7.vercraft.core.utils.ERROR_PREFIX
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import com.akuleshov7.vercraft.core.isValidSemVerFormat

const val ERROR_RELEASE_TYPE_PARSING = "$ERROR_PREFIX Invalid value for `$RELEASE_TYPE` property, " +
        "please check the value in `-P$RELEASE_TYPE`. " +
        "It can only be MAJOR, MINOR or PATCH"

const val ERROR_SEM_VER_PARSING = "$ERROR_PREFIX Invalid version is passed to `$SEM_VER` property, " +
        "please check the value in `-P$SEM_VER`."

open class VercraftExtension(objectFactory: ObjectFactory) {
    val logger = Logging.getLogger(VercraftExtension::class.java)
    var releaseType: Property<SemVerReleaseType> = objectFactory.property(SemVerReleaseType::class.java)
    var config: Property<Config> = objectFactory.property(Config::class.java)
    var semVer: Property<SemVer> = objectFactory.property(SemVer::class.java)

    fun setReleaseTypeFromProps(prop: Any?) {
        try {
            releaseType.set(
                prop?.toString()
                    ?.let { SemVerReleaseType.fromValue(it) }
                    ?: SemVerReleaseType.MINOR
            )
        } catch (e: IllegalArgumentException) {
            logger.error(ERROR_RELEASE_TYPE_PARSING)
            throw IllegalArgumentException(ERROR_RELEASE_TYPE_PARSING)
        }
    }

    fun setSemVerFromProps(prop: Any?) {
        semVer.set(
            prop?.toString()
                ?.let {
                    if (it.isValidSemVerFormat()) SemVer(it) else throw IllegalArgumentException(ERROR_SEM_VER_PARSING)
                }
        )
    }
}
