// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `kotlin-dsl`
  `java-gradle-plugin`
  `maven-publish`
  alias(libs.plugins.kotlinApiDump)
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_11)
  }
}

dependencies {
  implementation(gradleApi())
  implementation(localGroovy())
  implementation(libs.asm)
  implementation(libs.asm.util)
  implementation(libs.kotlin.gradle.plugin)
}

gradlePlugin {
  plugins {
    create("ktjni") {
      id = "io.github.fletchmckee.ktjni"
      implementationClass = "io.github.fletchmckee.ktjni.KtjniPlugin"
    }
  }
}
