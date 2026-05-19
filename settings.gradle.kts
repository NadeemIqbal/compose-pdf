pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "compose-pdf"

include(":compose-pdf")
include(":sample:desktopApp")
// v0.1 ships Desktop (JVM) only — uses Apache PDFBox under the hood. iOS / Web / Android are
// planned for v0.2 once Skiko's PDFDocument bindings stabilise (they're not in the public
// Kotlin API as of Skiko 0.10).
