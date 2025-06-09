// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni.internal

import io.github.fletchmckee.ktjni.tasks.KtjniTask
import io.github.fletchmckee.ktjni.util.GROUP
import io.github.fletchmckee.ktjni.util.taskSuffix
import io.github.fletchmckee.ktjni.util.titleCase
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile

internal fun Project.configureKotlinMultiplatform(
  headerOutputDir: DirectoryProperty,
  aggregate: ConfigurableFileCollection,
) {
  val kotlinExtension = extensions.getByType(KotlinMultiplatformExtension::class.java)
  kotlinExtension.targets.all {
    compilations.all {
      // The only KMP platforms we need registrations for are `jvm` or `androidJvm`.
      if (platformType.name == "jvm" || platformType.name == "androidJvm") {
        registerKotlinCompilationHeaderTask(
          compilation = this,
          headerOutputDir = headerOutputDir,
          aggregate = aggregate,
          target = target.name,
        )
      }
    }
  }
}

internal fun Project.configureKotlinJvm(
  headerOutputDir: DirectoryProperty,
  aggregate: ConfigurableFileCollection,
) {
  val kotlinExtension = extensions.getByType(KotlinJvmProjectExtension::class.java)
  kotlinExtension.target.compilations.all {
    registerKotlinCompilationHeaderTask(
      compilation = this,
      headerOutputDir = headerOutputDir,
      aggregate = aggregate,
    )
  }
}

internal fun Project.configureKotlinAndroid(
  headerOutputDir: DirectoryProperty,
  aggregate: ConfigurableFileCollection,
) {
  val kotlinExtension = extensions.getByType(KotlinAndroidProjectExtension::class.java)
  kotlinExtension.target.compilations.all {
    registerKotlinCompilationHeaderTask(
      compilation = this,
      headerOutputDir = headerOutputDir,
      aggregate = aggregate,
    )
  }
}

private fun Project.registerKotlinCompilationHeaderTask(
  compilation: KotlinCompilation<*>,
  headerOutputDir: DirectoryProperty,
  aggregate: ConfigurableFileCollection,
  target: String = "", // For Kotlin Multiplatform
) {
  val taskSuffix = compilation.name.taskSuffix(target)
  val taskName = "generateKotlin${taskSuffix.titleCase}JniHeaders"

  val generateJniHeadersTask = tasks.register(taskName, KtjniTask::class.java) {
    sourceDir.set(
      compilation.compileTaskProvider.flatMap { compileTask ->
        (compileTask as AbstractKotlinCompile<*>).destinationDirectory
      },
    )
    outputDir.set(headerOutputDir.map { it.dir("kotlin/$taskSuffix") })

    group = GROUP
    description = "Generates Kotlin JNI headers from class files for ${compilation.name} compilation"

    doFirst {
      logger.info("Ktjni - running $taskName")
    }
  }

  aggregate.from(generateJniHeadersTask.flatMap { it.outputDir })
}
