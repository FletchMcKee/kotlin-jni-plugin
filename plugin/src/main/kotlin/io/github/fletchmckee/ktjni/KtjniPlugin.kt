// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel.INFO
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile

@Suppress("unused") // Invoked reflectively
public class KtjniPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.gradle.projectsEvaluated {
      project.tasks
        .withType(AbstractKotlinCompile::class.java)
        .forEach { compileTask ->
          val classDir = compileTask.destinationDirectory
          val taskName = "generateJniHeaders${compileTask.name.replaceFirstChar(Char::uppercase)}"
          project.logger.log(INFO, "KtjniPlugin classDir: ${classDir.asFile.get().absolutePath} taskName: $taskName")
          project.tasks.register(taskName, GenerateJniHeaders::class.java) {
            sourceDir.set(classDir)
            outputDir.set(project.layout.buildDirectory.dir("generated/jni-headers/${compileTask.name}"))
            dependsOn(compileTask)
          }
        }
    }
  }
}
