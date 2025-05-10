// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.kotlinApiDump) apply false
  alias(libs.plugins.ktjni.root)
}

group = "io.github.fletchmckee"
version = "1.0.0-SNAPSHOT"
