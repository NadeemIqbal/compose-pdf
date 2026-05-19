import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vanniktech.maven.publish)
}

// Release version is driven by the git tag on CI: tag `v0.2.0` publishes `0.2.0`.
val libVersion: String =
    (System.getenv("RELEASE_VERSION") ?: findProperty("version") as String?)
        ?.removePrefix("v")
        ?.takeUnless { it.isBlank() || it == "unspecified" }
        ?: "0.1.0"

group = "io.github.nadeemiqbal"
version = libVersion

// v0.1 targets Desktop (JVM) only — uses Apache PDFBox under the hood, which is JVM-native and
// rock-solid. iOS + Web targets are planned for v0.2 once Skiko's PDFDocument bindings stabilise
// (they're not in the public Kotlin API as of Skiko 0.10).
kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate()

    jvm("desktop") {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                // Apache PDFBox 3.x — JVM-native PDF generator. ~5 MB, well-maintained, Apache 2.0.
                implementation("org.apache.pdfbox:pdfbox:3.0.5")
            }
        }
        // desktopTest inherits the desktopMain classpath (so PDFBox is already available
        // for the round-trip parse tests). No extra dependencies needed.
    }
}

mavenPublishing {
    publishToMavenCentral()

    if (
        project.hasProperty("signingInMemoryKey") ||
        project.hasProperty("signing.keyId")
    ) {
        signAllPublications()
    }

    coordinates("io.github.nadeemiqbal", "compose-pdf", libVersion)

    pom {
        name.set("ComposePdf")
        description.set(
            "PDF document builder for Compose Multiplatform apps — a DSL for laying out " +
                "multi-page PDFs (text, shapes, lines, images) via a familiar Compose-style API, " +
                "with selectable text and vector output. v0.1 targets Desktop (JVM, Apache " +
                "PDFBox under the hood); iOS / Web / Android are planned for v0.2.",
        )
        inceptionYear.set("2026")
        url.set("https://github.com/NadeemIqbal/compose-pdf")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("NadeemIqbal")
                name.set("Nadeem Iqbal")
                email.set("mr_nadeem_iqbal@yahoo.com")
                url.set("https://github.com/NadeemIqbal")
            }
        }
        scm {
            url.set("https://github.com/NadeemIqbal/compose-pdf")
            connection.set("scm:git:git://github.com/NadeemIqbal/compose-pdf.git")
            developerConnection.set("scm:git:ssh://git@github.com/NadeemIqbal/compose-pdf.git")
        }
    }
}
