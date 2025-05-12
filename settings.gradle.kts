// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
pluginManagement {
  includeBuild("build-support/settings")
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  id("io.github.fletchmckee.ktjni.settings")
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

includeBuild("build-support") {
  dependencySubstitution {
    substitute(module("io.github.fletchmckee.ktjni.build:gradle-plugin")).using(project(":"))
    substitute(module("io.github.fletchmckee.ktjni:plugin")).using(project(":plugin"))
  }
}
