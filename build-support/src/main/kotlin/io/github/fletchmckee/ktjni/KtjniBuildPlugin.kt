// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.configure

@Suppress("unused") // Invoked reflectively
class KtjniBuildPlugin : Plugin<Project> {
  override fun apply(target: Project) = with(target) {
    configureSpotless()
    configureTesting()
  }
}

private fun Project.configureSpotless() {
  with(pluginManager) { apply("com.diffplug.spotless") }

  spotless {
    val ktlintVersion = libs.findVersion("ktlint").get().requiredVersion

    kotlin {
      when {
        path == ":" -> target("build-support/src/**/*.kt", "build-support/settings/src/**/*.kt")
        else -> target("src/**/*.kt")
      }
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

private fun Project.configureTesting() {
  tasks.withType(AbstractTestTask::class.java).configureEach {
    testLogging {
      if (providers.environmentVariable("CI").isPresent) {
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED, TestLogEvent.PASSED)
      }
      exceptionFormat = TestExceptionFormat.FULL
    }
  }
}
