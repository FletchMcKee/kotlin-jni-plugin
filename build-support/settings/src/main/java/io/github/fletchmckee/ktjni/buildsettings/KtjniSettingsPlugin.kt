// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni.buildsettings

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings

@Suppress("unused") // Invoked reflectively
class KtjniSettingsPlugin : Plugin<Settings> {
  override fun apply(target: Settings) {
    val applyKtjniBuildPlugin: (Project) -> Unit = { project ->
      project.plugins.apply("io.github.fletchmckee.ktjni.build")
    }

    target.gradle.allprojects {
      when {
        // Root project needs to wait until after evaluation to apply plugin
        project.path == ":" -> project.afterEvaluate { applyKtjniBuildPlugin(project) }
        // Other projects apply plugin before evaluation
        else -> project.beforeEvaluate { applyKtjniBuildPlugin(project) }
      }
    }
  }
}
