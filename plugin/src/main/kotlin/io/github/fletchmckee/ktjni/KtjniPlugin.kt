// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import io.github.fletchmckee.ktjni.internal.PluginId
import io.github.fletchmckee.ktjni.internal.configureAndroidVariants
import io.github.fletchmckee.ktjni.internal.configureKotlinAndroid
import io.github.fletchmckee.ktjni.internal.configureKotlinJvm
import io.github.fletchmckee.ktjni.internal.configureKotlinMultiplatform
import io.github.fletchmckee.ktjni.tasks.KtjniTask
import io.github.fletchmckee.ktjni.util.GROUP
import io.github.fletchmckee.ktjni.util.titleCase
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.scala.ScalaCompile

@Suppress("unused") // Invoked reflectively
public class KtjniPlugin : Plugin<Project> {
  override fun apply(target: Project): Unit = with(target) {
    val extension = extensions.create("ktjni", KtjniExtension::class.java)
    // Connects all header task outputs as inputs to this aggregator task.
    val aggregate = objects.fileCollection()

    tasks.register("generateJniHeaders") {
      group = GROUP
      description = "Generates JNI headers for all JVM compile tasks"

      // Using `inputs.files` instead of `dependsOn` allows for better up-to-date checking and avoids eager task resolution.
      inputs.files(aggregate)
    }

    // This outputDir is optional so we set a default convention.
    val headerOutputDir = extension.outputDir.convention(project.layout.buildDirectory.dir("generated/ktjni"))

    configureAndroid(headerOutputDir, aggregate)
    configureKotlin(headerOutputDir, aggregate)
    configureJava(headerOutputDir, aggregate)
    configureScala(headerOutputDir, aggregate)
  }

  private fun Project.configureAndroid(
    headerOutputDir: DirectoryProperty,
    aggregate: ConfigurableFileCollection,
  ) {
    plugins.withId(PluginId.AndroidApplication.id) {
      configureAndroidVariants(headerOutputDir, aggregate)
    }

    plugins.withId(PluginId.AndroidLibrary.id) {
      configureAndroidVariants(headerOutputDir, aggregate)
    }
  }

  private fun Project.configureKotlin(
    headerOutputDir: DirectoryProperty,
    aggregate: ConfigurableFileCollection,
  ) {
    // Kotlin Multiplatform
    plugins.withId(PluginId.KotlinMultiplatform.id) {
      configureKotlinMultiplatform(headerOutputDir, aggregate)
    }

    // Kotlin JVM
    plugins.withId(PluginId.KotlinJvm.id) {
      configureKotlinJvm(headerOutputDir, aggregate)
    }

    // Kotlin Android
    plugins.withId(PluginId.KotlinAndroid.id) {
      configureKotlinAndroid(headerOutputDir, aggregate)
    }
  }

  private fun Project.configureJava(
    headerOutputDir: DirectoryProperty,
    aggregate: ConfigurableFileCollection,
  ) {
    plugins.withType(JavaBasePlugin::class.java) {
      val javaExtension = extensions.getByName("sourceSets") as SourceSetContainer
      javaExtension.all {
        val compileTaskProvider = tasks.named(compileJavaTaskName, JavaCompile::class.java)
        registerJavaHeaderTask(
          sourceSetName = name,
          compileTaskProvider = compileTaskProvider,
          headerOutputDir = headerOutputDir,
          aggregate = aggregate,
        )
      }
    }
  }

  private fun Project.configureScala(
    headerOutputDir: DirectoryProperty,
    aggregate: ConfigurableFileCollection,
  ) {
    plugins.withId(PluginId.Scala.id) {
      // The scala plugin also applies the java plugin.
      val sourceSets = extensions.getByName("sourceSets") as SourceSetContainer
      sourceSets.all {
        val compileTaskProvider = tasks.named(getCompileTaskName("scala"), ScalaCompile::class.java)
        registerScalaHeaderTask(
          sourceSetName = name,
          compileTaskProvider = compileTaskProvider,
          headerOutputDir = headerOutputDir,
          aggregate = aggregate,
        )
      }
    }
  }

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

  private fun Project.registerScalaHeaderTask(
    sourceSetName: String,
    compileTaskProvider: TaskProvider<ScalaCompile>,
    headerOutputDir: DirectoryProperty,
    aggregate: ConfigurableFileCollection,
  ) {
    val taskName = "generateScala${sourceSetName.titleCase}JniHeaders"
    val generateJniHeadersTask = tasks.register(taskName, KtjniTask::class.java) {
      sourceDir.set(
        compileTaskProvider.flatMap { it.destinationDirectory },
      )

      outputDir.set(headerOutputDir.map { it.dir("scala/$sourceSetName") })

      group = GROUP
      description = "Generates Scala JNI headers from class files for $sourceSetName compilation."

      doFirst {
        logger.info("Ktjni - running $taskName")
      }
    }

    aggregate.from(generateJniHeadersTask.flatMap { it.outputDir })
  }
}
