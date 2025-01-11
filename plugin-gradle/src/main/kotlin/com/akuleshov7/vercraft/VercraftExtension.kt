package com.akuleshov7.vercraft

import com.akuleshov7.vercraft.core.SemVerReleaseType
import com.akuleshov7.vercraft.core.utils.ERROR_PREFIX
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

const val ERROR_RELEASE_TYPE_PARSING = "$ERROR_PREFIX Invalid value for `releaseType` property, " +
        "please check the value in `-PreleaseType`. " +
        "It can only be MAJOR, MINOR or PATCH"

open class VercraftExtension(objectFactory: ObjectFactory) {
    val logger = Logging.getLogger(VercraftExtension::class.java)
    var releaseType: Property<SemVerReleaseType> = objectFactory.property(SemVerReleaseType::class.java)

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
}