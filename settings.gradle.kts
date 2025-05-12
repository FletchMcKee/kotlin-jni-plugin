// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
pluginManagement {
  includeBuild("build-logic")
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "ktjni"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
  ":plugin",
  ":samples:demo",
  ":samples:simple",
)
