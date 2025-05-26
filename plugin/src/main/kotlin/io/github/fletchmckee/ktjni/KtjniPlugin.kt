// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import io.github.fletchmckee.ktjni.internal.PluginId
import io.github.fletchmckee.ktjni.tasks.KtjniTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
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

      // Using `inputs.files` instead of `dependsOn` allows for better up-to-date checking and avoids eager task resolution that `dependsOn`
      // would cause.
      inputs.files(aggregate)
    }

    // TODO: Handle com.android.library and com.android.application
    configureKotlin(extension, aggregate)
    configureJava(extension, aggregate)
    configureScala(extension, aggregate)
  }

  private fun Project.configureKotlin(
    ktjniExtension: KtjniExtension,
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
              ktjniExtension = ktjniExtension,
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
          ktjniExtension = ktjniExtension,
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
          ktjniExtension = ktjniExtension,
          aggregate = aggregate,
        )
      }
    }
  }

  private fun Project.configureJava(
    extension: KtjniExtension,
    aggregate: ConfigurableFileCollection,
  ) {
    plugins.withType(JavaBasePlugin::class.java) {
      val javaExtension = extensions.getByName("sourceSets") as SourceSetContainer
      javaExtension.all {
        val compileTaskProvider = tasks.named(compileJavaTaskName, JavaCompile::class.java)
        registerJavaHeaderTask(
          sourceSetName = name,
          compileTaskProvider = compileTaskProvider,
          extension = extension,
          aggregate = aggregate,
        )
      }
    }
  }

  private fun Project.configureScala(
    extension: KtjniExtension,
    aggregate: ConfigurableFileCollection,
  ) {
    plugins.withId("scala") {
      // The scala plugin also applies the java plugin.
      val sourceSets = extensions.getByName("sourceSets") as SourceSetContainer
      sourceSets.all {
        val compileTaskProvider = tasks.named(getCompileTaskName("scala"), ScalaCompile::class.java)
        registerScalaHeaderTask(
          sourceSetName = name,
          compileTaskProvider = compileTaskProvider,
          extension = extension,
          aggregate = aggregate,
        )
      }
    }
  }

  private fun Project.registerKotlinCompilationHeaderTask(
    compilation: KotlinCompilation<*>,
    ktjniExtension: KtjniExtension,
    aggregate: ConfigurableFileCollection,
    target: String = "", // For Kotlin Multiplatform
  ) {
    val taskSuffix = compilation.name.taskSuffix(target)
    val taskName = "generateKotlin${taskSuffix.replaceFirstChar(Char::uppercase)}JniHeaders"

    val generateJniHeadersTask = tasks.register(taskName, KtjniTask::class.java) {
      sourceDir.set(
        compilation.compileTaskProvider.flatMap { compileTask ->
          (compileTask as AbstractKotlinCompile<*>).destinationDirectory
        },
      )
      outputDir.convention(
        ktjniExtension.outputDir.orElse(
          project.layout.buildDirectory.dir("generated/sources/headers/kotlin").map { baseDir ->
            baseDir.dir(taskSuffix)
          },
        ),
      )
      group = GROUP
      description = "Generates Kotlin JNI headers from class files for ${compilation.name} compilation"

      doFirst {
        logger.info("Running Kotlin $taskName with sourceDir: ${sourceDir.get().asFile.absolutePath}")
      }
    }

    aggregate.from(generateJniHeadersTask.flatMap { it.outputDir })
  }

  private fun Project.registerJavaHeaderTask(
    sourceSetName: String,
    compileTaskProvider: TaskProvider<JavaCompile>,
    extension: KtjniExtension,
    aggregate: ConfigurableFileCollection,
  ) {
    val taskName = "generateJava${sourceSetName.replaceFirstChar(Char::uppercase)}JniHeaders"

    val generateJniHeadersTask = tasks.register(taskName, KtjniTask::class.java) {
      sourceDir.set(
        compileTaskProvider.flatMap { it.destinationDirectory },
      )

      outputDir.convention(
        extension.outputDir.orElse(
          project.layout.buildDirectory.dir("generated/sources/headers/java/$sourceSetName"),
        ),
      )

      group = GROUP
      description = "Generates Java JNI headers from class files for $sourceSetName compilation."

      doFirst {
        logger.info("Running $taskName with sourceDir: ${sourceDir.get().asFile.absolutePath}")
      }
    }

    aggregate.from(generateJniHeadersTask.flatMap { it.outputDir })
  }

  private fun Project.registerScalaHeaderTask(
    sourceSetName: String,
    compileTaskProvider: TaskProvider<ScalaCompile>,
    extension: KtjniExtension,
    aggregate: ConfigurableFileCollection,
  ) {
    val taskName = "generateScala${sourceSetName.replaceFirstChar(Char::uppercase)}JniHeaders"
    val generateJniHeadersTask = tasks.register(taskName, KtjniTask::class.java) {
      sourceDir.set(
        compileTaskProvider.flatMap { it.destinationDirectory },
      )

      outputDir.convention(
        extension.outputDir.orElse(
          project.layout.buildDirectory.dir("generated/sources/headers/scala/$sourceSetName"),
        ),
      )

      group = GROUP
      description = "Generates Scala JNI headers from class files for $sourceSetName compilation."

      doFirst {
        logger.info("Running $taskName with sourceDir: ${sourceDir.get().asFile.absolutePath}")
      }
    }

    aggregate.from(generateJniHeadersTask.flatMap { it.outputDir })
  }

  private fun String.taskSuffix(target: String): String = if (target.isEmpty()) this else target + this.replaceFirstChar(Char::uppercase)

  internal companion object {
    internal const val GROUP = "ktjni"
  }
}
