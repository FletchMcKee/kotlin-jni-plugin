// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile

@Suppress("unused") // Invoked reflectively
public class KtjniPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    val extension = target.extensions.create("ktjni", KtjniExtension::class.java)

    val allHeaderOutputs = target.objects.fileCollection()

    target.tasks.register("generateJniHeaders") {
      group = GROUP
      description = "Generates JNI headers for all JVM compile tasks"

      inputs.files(allHeaderOutputs)
    }

    target.afterEvaluate {
      val ignoreBuildTypes = extension.ignoreBuildTypes.convention(emptyList<String>()).get()
      // KotlinCompile types extend from AbstractKotlinCompile and not AbstractCompile.
      project.tasks
        .withType(AbstractKotlinCompile::class.java)
        .filter { task -> ignoreBuildTypes.none { task.name.contains(it, ignoreCase = true) } }
        .forEach { compileTask ->
          registerHeaderTask(
            compileTask = compileTask,
            classDir = compileTask.destinationDirectory,
            extension = extension,
            aggregate = allHeaderOutputs,
          )
        }

      // AbstractCompile provides the rest of the JVM-based language compilation tasks.
      project.tasks
        .withType(AbstractCompile::class.java)
        .filter { task -> ignoreBuildTypes.none { task.name.contains(it, ignoreCase = true) } }
        .forEach { compileTask ->
          registerHeaderTask(
            compileTask = compileTask,
            classDir = compileTask.destinationDirectory,
            extension = extension,
            aggregate = allHeaderOutputs,
          )
        }
    }
  }

  private fun Project.registerHeaderTask(
    compileTask: Task,
    classDir: DirectoryProperty,
    extension: KtjniExtension,
    aggregate: ConfigurableFileCollection,
  ) {
    val taskName = "generateJniHeaders${compileTask.name.replaceFirstChar(Char::uppercase)}"
    val generateJniHeadersTask = tasks.register(
      taskName,
      KtjniTask::class.java,
    ) {
      sourceDir.set(classDir)
      outputDir.set(extension.outputDir.convention(layout.buildDirectory.dir("generated/sources/headers")))
      group = GROUP
      description = "Generates JNI headers from class files for ${compileTask.name}"
    }

    aggregate.from(generateJniHeadersTask.flatMap { it.outputDir })
  }

  internal companion object {
    internal const val GROUP = "ktjni"
  }
}
