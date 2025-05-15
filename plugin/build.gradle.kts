// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  `kotlin-dsl`
  `java-gradle-plugin`
  `maven-publish`
  alias(libs.plugins.binary.compatibility.validator)
}

kotlin {
  explicitApi()
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

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

dependencies {
  implementation(gradleApi())
  implementation(localGroovy())
  implementation(libs.asm)
  implementation(libs.asm.util)
  implementation(libs.kotlin.gradle.plugin)

  // Test libraries
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.kotlin.test.junit5)
  testImplementation(libs.google.truth)
  testImplementation(gradleTestKit())
}

gradlePlugin {
  plugins {
    create("ktjni") {
      id = "io.github.fletchmckee.ktjni"
      implementationClass = "io.github.fletchmckee.ktjni.KtjniPlugin"
    }
  }
}
