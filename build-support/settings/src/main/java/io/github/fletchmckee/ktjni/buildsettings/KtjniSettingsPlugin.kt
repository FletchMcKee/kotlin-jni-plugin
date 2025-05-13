// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni.buildsettings

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.internal.cc.base.logger

@Suppress("unused") // Invoked reflectively
class KtjniSettingsPlugin : Plugin<Settings> {
  override fun apply(target: Settings) {
    val applyKtjniBuildPlugin: (Project) -> Unit = { project ->
      project.plugins.apply("io.github.fletchmckee.ktjni.build")
    }

    target.gradle.allprojects {
      logger.quiet("✅ settings $target path: ${project.path}")
      if (project.path == ":") {
        // Root project needs to wait until after evaluation to apply plugin
        project.afterEvaluate {
          applyKtjniBuildPlugin(project)
        }
      } else {
        // Other projects apply plugin before evaluation
        project.beforeEvaluate {
          applyKtjniBuildPlugin(project)
        }
      }
    }
  }
}
