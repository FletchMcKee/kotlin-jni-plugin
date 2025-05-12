// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel.INFO

@Suppress("unused") // Invoked reflectively
class KtjniBuildPlugin : Plugin<Project> {
  override fun apply(target: Project) =
    with(target) {
      logger.log(INFO, "✅ KtjniBuildPlugin applied to ${project.path}")
      configureSpotless()
    }
}
