// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0

plugins {
  alias(libs.plugins.ktjni.root)
  // id("io.github.fletchmckee.ktjni")
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.kotlinApiDump) apply false
}
