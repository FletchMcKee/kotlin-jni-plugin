// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.LogLevel.INFO
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

@CacheableTask
internal abstract class GenerateJniHeaders
@Inject
constructor() : DefaultTask() {
  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val sourceDir: DirectoryProperty

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @TaskAction
  fun generateJniHeaders() {
    val srcDir = sourceDir.asFile.get()
    logger.log(INFO, "Scanning: ${srcDir.absolutePath}")
    sourceDir.get().asFile
      .walkTopDown()
      .filter { it.extension == "class" }
      .forEach { classFile ->
        val reader = ClassReader(classFile.readBytes())
        val classNode = ClassNode()
        reader.accept(classNode, 0)

        val className = classNode.name.replace(File.separator, ".")

        for (method in classNode.methods) {
          if ((method.access and Opcodes.ACC_NATIVE) != 0) {
            logger.log(INFO, "Native method: $className#${method.name}${method.desc}")
          }
        }
      }
  }
}
