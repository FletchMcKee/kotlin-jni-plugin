// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
allprojects {
  repositories {
    mavenCentral()
    google()
  }
}

plugins {
  `kotlin-dsl`
  alias(libs.plugins.spotless)
}

dependencies {
  compileOnly(libs.spotless.gradle.plugin)
}

gradlePlugin {
  plugins {
    register("ktjniBuild") {
      id = "io.github.fletchmckee.ktjni.build"
      implementationClass = "io.github.fletchmckee.ktjni.KtjniBuildPlugin"
    }
  }
}
