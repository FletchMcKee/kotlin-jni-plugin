// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni.internal

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ComponentIdentity
import com.android.build.api.variant.HasAndroidTest
import com.android.build.api.variant.HasUnitTest
import io.github.fletchmckee.ktjni.tasks.KtjniTask
import io.github.fletchmckee.ktjni.util.GROUP
import io.github.fletchmckee.ktjni.util.titleCase
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile

internal fun Project.configureAndroidVariants(
  headerOutputDir: DirectoryProperty,
  aggregate: ConfigurableFileCollection,
) {
  val androidExtension = extensions.getByType(AndroidComponentsExtension::class.java)
  androidExtension.onVariants { variant ->
    findJavaCompilationTask(variant, headerOutputDir, aggregate)
    (variant as? HasUnitTest)?.unitTest?.let {
      findJavaCompilationTask(it, headerOutputDir, aggregate)
    }
    (variant as? HasAndroidTest)?.androidTest?.let {
      findJavaCompilationTask(it, headerOutputDir, aggregate)
    }
  }
}

private fun Project.findJavaCompilationTask(
  variant: ComponentIdentity,
  headerOutputDir: DirectoryProperty,
  aggregate: ConfigurableFileCollection,
) {
  // Generally trying to avoid string matching for task types, but this is a pattern you'll see in other plugins like room-gradle-plugin
  // See AndroidPluginIntegration.kt
  val compileTaskName = "compile${variant.name.titleCase}JavaWithJavac"
  tasks.withType(JavaCompile::class.java).all {
    if (name == compileTaskName) {
      val compileTaskProvider = tasks.named(compileTaskName, JavaCompile::class.java)
      registerJavaHeaderTask(
        sourceSetName = variant.name,
        compileTaskProvider = compileTaskProvider,
        headerOutputDir = headerOutputDir,
        aggregate = aggregate,
      )
    }
  }
}

// For now we'll have duplicate methods until I can figure out a better way to clean this up.
private fun Project.registerJavaHeaderTask(
  sourceSetName: String,
  compileTaskProvider: TaskProvider<JavaCompile>,
  headerOutputDir: DirectoryProperty,
  aggregate: ConfigurableFileCollection,
) {
  val taskName = "generateJava${sourceSetName.titleCase}JniHeaders"

  val generateJniHeadersTask = tasks.register(taskName, KtjniTask::class.java) {
    sourceDir.set(
      compileTaskProvider.flatMap { it.destinationDirectory },
    )

    outputDir.set(headerOutputDir.map { it.dir("java/$sourceSetName") })

    group = GROUP
    description = "Generates Java JNI headers from class files for $sourceSetName compilation."

    doFirst {
      logger.info("Ktjni - running $taskName")
    }
  }

  aggregate.from(generateJniHeadersTask.flatMap { it.outputDir })
}
