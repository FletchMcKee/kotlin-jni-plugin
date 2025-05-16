// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.AbstractCompile
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
      // KotlinCompile types extend from AbstractKotlinCompile and not AbstractCompile.
      project.tasks
        .withType(AbstractKotlinCompile::class.java)
        .filter { task ->
          // Keep task if it doesn't match any ignored build type
          ignoreBuildTypes.none { buildType ->
            task.name.contains(buildType, ignoreCase = true)
          }
        }
        .forEach { compileTask ->
          registerHeaderTask(
            name = compileTask.name,
            compileTask = AbstractKotlinCompile::class.java,
            classDir = compileTask.destinationDirectory,
            extension = extension,
            aggregate = aggregate,
          )
        }

      project.tasks
        .withType(AbstractCompile::class.java)
        .filter { task ->
          // Keep task if it doesn't match any ignored build type
          ignoreBuildTypes.none { buildType ->
            task.name.contains(buildType, ignoreCase = true)
          }
        }
        .forEach { compileTask ->
          registerHeaderTask(
            name = compileTask.name,
            compileTask = AbstractCompile::class.java,
            classDir = compileTask.destinationDirectory,
            extension = extension,
            aggregate = aggregate,
          )
        }
    }
  }

  private fun <T : Task> Project.registerHeaderTask(
    name: String,
    compileTask: Class<T>,
    classDir: DirectoryProperty,
    extension: KtjniExtension,
    aggregate: TaskProvider<Task>,
  ) {
    val compileTaskProvider = tasks.named(name, compileTask)
    val taskName = "generateJniHeaders${name.replaceFirstChar(Char::uppercase)}"
    val generateJniHeadersTask = tasks.register(
      taskName,
      KtjniTask::class.java,
    ) {
      sourceDir.set(classDir)
      outputDir.set(extension.outputDir.convention(layout.buildDirectory.dir("generated/ktjni")))
      group = GROUP
      description = "Generates JNI headers from class files after $name"
    }

    generateJniHeadersTask.configure { dependsOn(compileTaskProvider) }

    aggregate.configure { dependsOn(generateJniHeadersTask) }
  }

  internal companion object {
    internal const val GROUP = "ktjni"
  }
}
