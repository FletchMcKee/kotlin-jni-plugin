// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `kotlin-dsl`
  `java-gradle-plugin`
  alias(libs.plugins.binary.compatibility.validator)
  alias(libs.plugins.maven.publish)
}

kotlin {
  explicitApi()
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile> {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_11)
  }
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}

val testKitRuntimeOnly by configurations.registering

dependencies {
  // These need to be bundled with the plugin.
  implementation(libs.asm)
  implementation(libs.asm.tree)

  // Provided by Gradle runtime or the user's own environment.
  compileOnly(libs.kotlin.gradle.plugin)
  compileOnly(libs.android.gradle.plugin)
  compileOnly(gradleApi())
  compileOnly(localGroovy())

  // Test libraries
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.junit.jupiter.params)
  testImplementation(libs.kotlin.test.junit5)
  testImplementation(libs.google.truth)
  testImplementation(gradleTestKit())

  testKitRuntimeOnly(libs.kotlin.gradle.plugin)
  testKitRuntimeOnly(libs.android.gradle.plugin)
  testRuntimeOnly(libs.junit.platform.launcher)
}

// `pluginUnderTestMetadata` automatically builds a plugin classpath from implementation dependencies, but it doesn’t include transitive
// runtime-only dependencies like Kotlin's gradle-plugin, so we set it here.
tasks.named<PluginUnderTestMetadata>("pluginUnderTestMetadata") {
  pluginClasspath.from(testKitRuntimeOnly)
}

gradlePlugin {
  plugins {
    create("ktjni") {
      id = "io.github.fletchmckee.ktjni"
      implementationClass = "io.github.fletchmckee.ktjni.KtjniPlugin"
    }
  }
}

// This module exists in two contexts:
// 1. In the root project where it's built as a publishable artifact
// 2. In the build-support includeBuild where it's used for internal development
if (rootProject.name == "ktjni") {
  mavenPublishing {
    configure(
      GradlePlugin(
        javadocJar = JavadocJar.Javadoc(),
        sourcesJar = true,
      ),
    )
  }
} else {
  // Use a separate build directory when included in build-support to prevent build cache conflicts and configuration pollution between
  // contexts.
  layout.buildDirectory.set(File(rootProject.rootDir, "build/plugin"))
}
