// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

fun Project.configureSpotless() {
  with(pluginManager) {
    apply("com.diffplug.spotless")
  }

  spotless {
    val ktlintVersion = libs.findVersion("ktlint").get().requiredVersion

    kotlin {
      target("**/*.kt")
      ktlint(ktlintVersion).editorConfigOverride(
        mapOf(
          "ktlint_standard_filename" to "disabled",
          "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
        ),
      )
      licenseHeaderFile(rootProject.file("spotless/copyright.txt"))
    }

    kotlinGradle {
      target("*.kts")
      ktlint(ktlintVersion)
      licenseHeaderFile(rootProject.file("spotless/copyright.txt"), "(^(?![\\/ ]\\**).*$)")
    }
  }
}

private fun Project.spotless(action: SpotlessExtension.() -> Unit) = extensions.configure<SpotlessExtension>(action)
