// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile

@Suppress("unused") // Invoked reflectively
public class KtjniPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val extension = project.extensions.create("ktjni", KtjniExtension::class.java)

    val aggregate = project.tasks.register("generateJniHeaders") {
      group = GROUP
      description = "Generates JNI headers for all Kotlin compile tasks"
    }

    project.afterEvaluate {
      val ignoreBuildTypes = extension.ignoreBuildTypes.convention(emptyList<String>()).get()
      project.tasks
        .withType(AbstractKotlinCompile::class.java)
        .filter { task ->
          // Keep task if it doesn't match any ignored build type
          ignoreBuildTypes.none { buildType ->
            task.name.contains(buildType, ignoreCase = true)
          }
        }
        .forEach { compileTask ->
          val compileTaskProvider = project.tasks.named(compileTask.name, AbstractKotlinCompile::class.java)
          val classDir = compileTask.destinationDirectory
          val taskName = "generateJniHeaders${compileTask.name.replaceFirstChar(Char::uppercase)}"
          val generateJniHeadersTask = project.tasks.register(
            taskName,
            KtjniTask::class.java,
          ) {
            sourceDir.set(classDir)
            outputDir.set(extension.outputDir.convention(project.layout.buildDirectory.dir("generated/ktjni")))
            group = GROUP
            description = "Generates JNI headers from class files after ${compileTask.name}"
          }

          generateJniHeadersTask.configure { dependsOn(compileTaskProvider) }

          aggregate.configure { dependsOn(generateJniHeadersTask) }
        }
    }
  }

  internal companion object {
    internal const val GROUP = "ktjni"
  }
}
