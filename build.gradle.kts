// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
buildscript {
  dependencies {
    classpath(libs.ktjni.build.plugin)
    classpath(libs.ktjni.gradle.plugin)
  }
  repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
  }
}

plugins {
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.binary.compatibility.validator) apply false
}
