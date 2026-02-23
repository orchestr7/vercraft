/**
 * Publishing configuration file.
 */

@file:Suppress(
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION",
)

package com.akuleshov7.buildutils

import io.github.gradlenexus.publishplugin.NexusPublishExtension
import io.github.gradlenexus.publishplugin.NexusPublishPlugin
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.dokka.gradle.DokkaPlugin

/**
 * Configures all aspects of the publishing process.
 */
fun Project.configurePublishing() {
    if (this == rootProject) {
        apply<MavenPublishPlugin>()
        configureNexusPublishing()
    }

    afterEvaluate {
        configureSigning()
    }
}

/**
 * Configures _pom.xml_
 *
 * @param project
 */
@Suppress("TOO_LONG_FUNCTION")
fun MavenPom.configurePom(project: Project) {
    name.set(project.name)
    description.set(project.description ?: project.name)
    url.set("https://github.com/orchestr7/vercraft")
    licenses {
        license {
            name.set("MIT License")
            url.set("https://opensource.org/license/MIT")
            distribution.set("repo")
        }
    }
    developers {
        developer {
            id.set("akuleshov7")
            name.set("Andrey Kuleshov")
            email.set("andrewkuleshov7@gmail.com")
            url.set("https://github.com/akuleshov7")
        }
    }
    scm {
        url.set("https://github.com/orchestr7/vercraft")
        connection.set("scm:git:git://github.com/orchestr7/vercraft.git")
        developerConnection.set("scm:git:git@github.com:orchestr7/vercraft.git")
    }
}

/**
 * Configures all publications. The publications must already exist.
 */
fun Project.configureArtifact() {
    if (this == rootProject) {
        return
    }
    // adding sources to release
    val sources = tasks.register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from("src/main/kotlin")
    }
    // adding documentation to release
    apply<DokkaPlugin>()
    val dokkaJarProvider = tasks.register<Jar>("dokkaJar") {
        group = "documentation"
        archiveClassifier.set("javadoc")
        from(tasks.named("dokkaHtml"))
    }
    configure<PublishingExtension> {
        repositories {
            mavenLocal()
        }
        publications.withType<MavenPublication>().configureEach {
            artifact(sources)
            artifact(dokkaJarProvider)
            pom {
                configurePom(project)
            }
        }
    }
}

/**
 * Configures Maven Central as the publish destination.
 */
@Suppress("TOO_LONG_FUNCTION")
private fun Project.configureNexusPublishing() {
    setPropertyFromEnv("SONATYPE_USER", "sonatypeUsername")
    setPropertyFromEnv("SONATYPE_PASSWORD", "sonatypePassword")

    if (!hasProperties("sonatypeUsername", "sonatypePassword")) {
        println("Skipping Nexus publishing configuration as either sonatypeUsername or sonatypePassword are not set")
        return
    }

    apply<NexusPublishPlugin>()

    configure<NexusPublishExtension> {
        repositories {
            sonatype {
                nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
                snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
                username.set(property("sonatypeUsername") as String)
                password.set(property("sonatypePassword") as String)
            }
        }
    }
}


/**
 * Enables signing of the artifacts if the `signingKey` project property is set.
 *
 * Should be explicitly called after each custom `publishing {}` section.
 */
private fun Project.configureSigning() {
    setPropertyFromEnv("PGP_SEC", "signingKey")
    setPropertyFromEnv("PGP_PASSWORD", "signingPassword")

    if (hasProperty("signingKey")) {
        // signing only for GitHub Actions
        configureSigningCommon {
            useInMemoryPgpKeys(property("signingKey") as String?, findProperty("signingPassword") as String?)
        }
    } else {
        tasks.withType<Sign> {
            isEnabled = false
            isRequired = false
        }
    }
}

/**
 * @param useKeys the block which configures the PGP keys. Use either
 *   [SigningExtension.useInMemoryPgpKeys], [SigningExtension.useGpgCmd], or an
 *   empty lambda.
 * @see SigningExtension.useInMemoryPgpKeys
 * @see SigningExtension.useGpgCmd
 */
private fun Project.configureSigningCommon(useKeys: SigningExtension.() -> Unit = {}) {
    apply<SigningPlugin>()
    configure<SigningExtension> {
        useKeys()
        val publications = extensions.getByType<PublishingExtension>().publications
        val publicationCount = publications.size
        println("The following $publicationCount publication(s) are getting signed: ${publications.map(Named::getName)}")
        sign(*publications.toTypedArray())
    }
    tasks.withType<PublishToMavenRepository>().configureEach {
        dependsOn(tasks.withType<Sign>())
    }
}

/**
 * Determines if this project has all the given properties.
 *
 * @param propertyNames the names of the properties to locate.
 * @return `true` if this project has all the given properties, `false` otherwise.
 * @see Project.hasProperty
 */
private fun Project.hasProperties(vararg propertyNames: String): Boolean = propertyNames.asSequence().all(this::hasProperty)

private fun Project.setPropertyFromEnv(envName: String, propertyName: String) {
    System.getenv(envName)?.let {
        extra.set(propertyName, it)
    }
}
