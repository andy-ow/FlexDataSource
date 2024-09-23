import java.io.FileInputStream
import java.util.Properties

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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        repositories {
            maven {
                val githubProperties = Properties()
                githubProperties.load(FileInputStream("/home/andrzej/programowanie/android/github.properties"))
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/andy-ow/FlexDataSource")
                credentials {
                    username = githubProperties["gpr.user"] as String? ?: System.getenv("GPR_USER")
                    password =
                        githubProperties["gpr.key"] as String? ?: System.getenv("GPR_API_KEY")
                }
            }
        }
    }
}

rootProject.name = "FlexDataSource"
include(":app")
include(":flexds-core")
include(":flexds-fb-storage")
include(":flexds-fb-realtime")
