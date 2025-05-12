package io.github.fletchmckee.ktjni

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel.INFO

class RootConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) = with(target) {
    logger.log(INFO, "RootConventionPlugin applied to ${project.path}")
    configureSpotless()
  }
}
