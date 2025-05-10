// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
plugins {
  `kotlin-dsl`
  `java-gradle-plugin`
  `maven-publish`
  kotlin("jvm") version "2.1.20"
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.ktjni.root)
}

group = "io.github.fletchmckee"
version = "1.0.0-SNAPSHOT"

dependencies {
  implementation(gradleApi())
  implementation(localGroovy())
  implementation("org.ow2.asm:asm:9.7")
  implementation("org.ow2.asm:asm-util:9.8")
}
