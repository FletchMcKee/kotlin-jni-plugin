// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.scala.ScalaCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("unused") // Invoked reflectively
public class KtjniPlugin : Plugin<Project> {
  override fun apply(target: Project): Unit = with(target) {
    val extension = extensions.create("ktjni", KtjniExtension::class.java)

    val allHeaderOutputs = objects.fileCollection()

    tasks.register("generateJniHeaders") {
      group = GROUP
      description = "Generates JNI headers for all JVM compile tasks"

      // This connects all header task outputs as inputs to this aggregator task. Using `inputs.files` instead of `dependsOn` allows for
      // better up-to-date checking and avoids eager task resolution that `dependsOn` would cause.
      inputs.files(allHeaderOutputs)
    }

    // KotlinCompile types extend from AbstractKotlinCompile and not AbstractCompile.
    tasks.withType(AbstractKotlinCompile::class.java) {
      registerHeaderTask(
        compileTask = this,
        classDir = destinationDirectory,
        extension = extension,
        aggregate = allHeaderOutputs,
      )
    }

    // AbstractCompile provides the rest of the JVM-based language compilation tasks.
    tasks.withType(AbstractCompile::class.java) {
      registerHeaderTask(
        compileTask = this,
        classDir = destinationDirectory,
        extension = extension,
        aggregate = allHeaderOutputs,
      )
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
      // If the user specified an outputDir in the extension, use that. Otherwise, use a default buildDirectory based on the language.
      // This lazy property resolution is deferred until execution time.
      outputDir.convention(
        extension.outputDir.orElse(
          project.layout.buildDirectory.dir("generated/sources/headers").map { it.dir(compileTask.languageName) },
        ),
      )
      group = GROUP
      description = "Generates JNI headers from class files for ${compileTask.name}"

      doFirst {
        logger.info("Running $taskName with sourceDir: ${sourceDir.get().asFile.absolutePath}")
      }
    }

    // Add this task's output to the aggregator collection. Using `flatMap` preserves the lazy/deferred property resolution pattern.
    aggregate.from(generateJniHeadersTask.flatMap { it.outputDir })
  }

  // This is a temporary hack until I come up with a better option. Also would be preferable to include source set.
  private val Task.languageName: String get() = when (this) {
    is KotlinCompile -> "kotlin"
    is JavaCompile -> "java"
    is ScalaCompile -> "scala"
    is GroovyCompile -> "groovy"
    else -> "jvm"
  }

  internal companion object {
    internal const val GROUP = "ktjni"
  }
}
