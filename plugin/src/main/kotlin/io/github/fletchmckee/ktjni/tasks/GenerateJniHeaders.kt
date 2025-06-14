// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni.tasks

import io.github.fletchmckee.ktjni.internal.toMangledJniName
import io.github.fletchmckee.ktjni.internal.writeEpilogue
import io.github.fletchmckee.ktjni.internal.writeNativeMethods
import io.github.fletchmckee.ktjni.internal.writePrologue
import io.github.fletchmckee.ktjni.internal.writeStatics
import io.github.fletchmckee.ktjni.util.isLocal
import io.github.fletchmckee.ktjni.util.needsHeader
import java.io.File
import java.io.FileInputStream
import java.io.PrintWriter
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logging
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

internal interface GenerateJniHeadersParams : WorkParameters {
  val sourceDir: DirectoryProperty
  val outputDir: DirectoryProperty
}

/**
 * See [JNI naming convention](https://docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/design.html#resolving_native_method_names)
 * for reference.
 */
internal abstract class GenerateJniHeaders : WorkAction<GenerateJniHeadersParams> {
  private val logger = Logging.getLogger(GenerateJniHeaders::class.java)

  override fun execute() {
    val srcDir = parameters.sourceDir.asFile.get()
    val outputDir = parameters.outputDir.asFile.get()
    // Removes stale files and also prevents creating empty directories for sourceSets that contain no external native methods.
    outputDir.deleteRecursively()
    val start = System.currentTimeMillis()
    logger.info("Ktjni - generating JNI headers for $srcDir")
    srcDir.walkTopDown()
      .filter { it.extension == "class" }
      .mapNotNull { classFile ->
        processClassFile(
          classFile = classFile,
          srcDir = srcDir,
          outputDir = outputDir,
        )
      }
      .count()
      .also { count ->
        val delta = System.currentTimeMillis() - start
        logger.info("Ktjni - completed writing $count header file(s) in $delta ms")
      }
  }

  private fun processClassFile(classFile: File, srcDir: File, outputDir: File): String? {
    // Parse to an ASM ClassNode
    val classNode = FileInputStream(classFile).use { inputStream ->
      val reader = ClassReader(inputStream)
      val node = ClassNode()
      reader.accept(node, 0)
      node
    }
    logger.info("Ktjni - processing class: ${classNode.name}")
    // JNI does not include native methods in local classes.
    if (classNode.isLocal) return null

    // Find native methods and track method overloading
    val overloadedMethodMap = mutableMapOf<String, Int>()
    val nativeMethods = classNode.methods
      .filter { it.needsHeader }
      .onEach { overloadedMethodMap.merge(it.name, 1, Int::plus) }

    if (nativeMethods.isEmpty()) return null

    return writeJniHeader(
      classNode = classNode,
      srcDir = srcDir,
      outputDir = outputDir,
      nativeMethods = nativeMethods,
      overloadedMethodMap = overloadedMethodMap,
    )
  }

  private fun writeJniHeader(
    classNode: ClassNode,
    srcDir: File,
    outputDir: File,
    nativeMethods: List<MethodNode>,
    overloadedMethodMap: Map<String, Int>,
  ): String? {
    val className = classNode.name.replace('/', '.')
    val fileName = className.replace(Regex("[.$]"), "_") + ".h"
    logger.info("Ktjni - class {$className} contains native methods. Creating file $fileName")
    // Necessary since we delete the outputDir recursively at the start of execution.
    outputDir.mkdirs()

    val file = File(outputDir, fileName)
    PrintWriter(file).use { out ->
      with(out) {
        val cName = className.toMangledJniName()
        writePrologue(cName)
        writeStatics(
          classNode = classNode,
          cName = cName,
          srcDir = srcDir,
        )
        writeNativeMethods(
          cName = cName,
          classFlatName = className,
          nativeMethods = nativeMethods,
          overloadedMethodMap = overloadedMethodMap,
        )
        writeEpilogue()
      }
    }

    return className
  }
}
