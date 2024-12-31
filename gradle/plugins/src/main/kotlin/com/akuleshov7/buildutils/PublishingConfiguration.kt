/**
 * Publishing configuration file.
 */

package com.akuleshov7.buildutils

import gradle.kotlin.dsl.accessors._76a779107637b25b34866585d88a55c4.signing
import io.github.gradlenexus.publishplugin.NexusPublishExtension
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.dokka.gradle.DokkaPlugin

/**
 * Enables signing of the artifacts if the `signingKey` project property is set.
 *
 * Should be explicitly called after each custom `publishing {}` section.
 */
fun Project.configureSigning() {
    if (gradle.startParameter.taskNames.contains("publishToMavenLocal")) {
        return
    }

    if (hasProperty("signingKey")) {
        configureSigningCommon {
            useInMemoryPgpKeys(property("signingKey") as String?, findProperty("signingPassword") as String?)
        }
    }
    // a hack for Gradle 8 (which is now requiring explicit dependency on sequential tasks with a specific order)
    val signingTasks = tasks.filter { it.name.startsWith("sign") && it.name.endsWith("Publication") }
    tasks.matching { it.name.startsWith("publish") }.configureEach {
        signingTasks.forEach {
            mustRunAfter(it.name)
        }
    }
}

internal fun Project.configurePublications() {
    configure<PublishingExtension> {
        repositories {
            mavenLocal()
        }

        apply<DokkaPlugin>()
        val dokka = tasks.register<Jar>("dokkaJar") {
            group = "documentation"
            archiveClassifier.set("javadoc")
            from(tasks.named("dokkaHtml"))
        }

        val sources = tasks.register<Jar>("sourcesJar") {
            archiveClassifier.set("sources")
            from("src/main/kotlin")
        }
        publications.withType<MavenPublication>().configureEach {
            this.artifact(sources)
            this.artifact(dokka)
            this.pom {
                name.set(project.name)
                description.set(project.description ?: project.name)
                url.set("https://github.com/orchestr7/vercraft")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/orchestr7/vercraft/blob/main/LICENSE")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("akuleshov7")
                        name.set("Andrey Kuleshov")
                        email.set("andrewkuleshov7@gmail.com")
                    }
                }
                scm {
                    url.set("https://github.com/orchestr7/vercraft")
                    connection.set("scm:git:git://github.com/orchestr7/vercraft.git")
                }
            }
        }
    }
}

internal fun Project.configureNexusPublishing() {
    configure<NexusPublishExtension> {
        repositories {
            sonatype {
                nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
                snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
                username.set(property("sonatypeUsername") as String)
                password.set(property("sonatypePassword") as String)
            }
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
    configure<SigningExtension> {
        useKeys()
        val publications = extensions.getByType<PublishingExtension>().publications
        val publicationCount = publications.size
        println("The following $publicationCount publication(s) are getting signed: ${publications.map(Named::getName)}")
        sign(*publications.toTypedArray())
    }
}
