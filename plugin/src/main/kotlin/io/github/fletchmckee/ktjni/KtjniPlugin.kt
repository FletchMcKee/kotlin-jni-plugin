// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ComponentIdentity
import com.android.build.api.variant.HasAndroidTest
import com.android.build.api.variant.HasUnitTest
import io.github.fletchmckee.ktjni.internal.PluginId
import io.github.fletchmckee.ktjni.tasks.KtjniTask
import io.github.fletchmckee.ktjni.util.taskSuffix
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
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile

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
      val androidExtension = extensions.getByType(AndroidComponentsExtension::class.java)
      androidExtension.onVariants { variant ->
        configureAndroidVariant(variant, headerOutputDir, aggregate)
        (variant as? HasUnitTest)?.unitTest?.let {
          configureAndroidVariant(it, headerOutputDir, aggregate)
        }
        (variant as? HasAndroidTest)?.androidTest?.let {
          configureAndroidVariant(it, headerOutputDir, aggregate)
        }
      }
    }

    plugins.withId(PluginId.AndroidLibrary.id) {
      val androidExtension = extensions.getByType(AndroidComponentsExtension::class.java)
      androidExtension.onVariants { variant ->
        configureAndroidVariant(variant, headerOutputDir, aggregate)
        (variant as? HasUnitTest)?.unitTest?.let {
          configureAndroidVariant(it, headerOutputDir, aggregate)
        }
        (variant as? HasAndroidTest)?.androidTest?.let {
          configureAndroidVariant(it, headerOutputDir, aggregate)
        }
      }
    }
  }

  private fun Project.configureAndroidVariant(
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

  private fun Project.configureKotlin(
    headerOutputDir: DirectoryProperty,
    aggregate: ConfigurableFileCollection,
  ) {
    // Kotlin Multiplatform
    plugins.withId(PluginId.KotlinMultiplatform.id) {
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

    // Kotlin JVM
    plugins.withId(PluginId.KotlinJvm.id) {
      val kotlinExtension = extensions.getByType(KotlinJvmProjectExtension::class.java)
      kotlinExtension.target.compilations.all {
        registerKotlinCompilationHeaderTask(
          compilation = this,
          headerOutputDir = headerOutputDir,
          aggregate = aggregate,
        )
      }
    }

    // Kotlin Android
    plugins.withId(PluginId.KotlinAndroid.id) {
      val kotlinExtension = extensions.getByType(KotlinAndroidProjectExtension::class.java)
      kotlinExtension.target.compilations.all {
        registerKotlinCompilationHeaderTask(
          compilation = this,
          headerOutputDir = headerOutputDir,
          aggregate = aggregate,
        )
      }
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

  internal companion object {
    internal const val GROUP = "ktjni"
  }
}
