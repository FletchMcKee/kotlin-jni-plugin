// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("NOTHING_TO_INLINE")

package io.github.fletchmckee.ktjni.internal

import io.github.fletchmckee.ktjni.util.isAsciiAlphanumeric
import io.github.fletchmckee.ktjni.util.isStatic
import io.github.fletchmckee.ktjni.util.isStaticFinal
import io.github.fletchmckee.ktjni.util.jniConstant
import io.github.fletchmckee.ktjni.util.jniType
import io.github.fletchmckee.ktjni.util.orZero
import java.io.File
import java.io.FileInputStream
import java.io.PrintWriter
import kotlin.text.iterator
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

/**
 * The javac
 * [JNIWriter](https://github.com/openjdk/jdk/blob/master/src/jdk.compiler/share/classes/com/sun/tools/javac/jvm/JNIWriter.java#L223-L226)
 * class split these up into four separate methods, but it's simplified here as everything before statics and native methods.
 */
internal fun PrintWriter.writePrologue(cName: String) {
  println("/* DO NOT EDIT THIS FILE - it is machine generated */")
  println("#include <jni.h>")
  println("/* Header for class $cName */")
  println()
  println("#ifndef _Included_$cName")
  println("#define _Included_$cName")
  println("#ifdef __cplusplus")
  println("extern \"C\" {")
  println("#endif")
  println() // Makes it easier to read.
}

/**
 * [JNIWriter](https://github.com/openjdk/jdk/blob/master/src/jdk.compiler/share/classes/com/sun/tools/javac/jvm/JNIWriter.java#L231-L232)
 */
internal fun PrintWriter.writeEpilogue() {
  println("#ifdef __cplusplus")
  println("}")
  println("#endif")
  println("#endif")
}

/**
 * Writes #define directives for static final constants in a class to the JNI header.
 *
 * This function generates C preprocessor macros for all static final constants
 * in the specified class. Each constant is encoded according to JNI conventions:
 * - First creates an #undef directive to avoid potential redefinition issues
 * - Then creates a #define directive with the properly encoded constant value
 * - Constants are named in the format: ClassName_FieldName
 *
 * Only constants with primitive types and strings are supported.
 *
 * Example output:
 * ```
 * #undef com_example_MyClass_VERSION
 * #define com_example_MyClass_VERSION 42L
 * ```
 *
 * @param classNode The ASM ClassNode containing the fields to process
 * @param cName The JNI-mangled class name
 */
internal fun PrintWriter.writeStatics(classNode: ClassNode, cName: String, srcDir: File) {
  val classHierarchy = buildClassHierarchy(classNode, srcDir)
  for (node in classHierarchy) {
    node.fields
      .filter { it.isStaticFinal }
      .forEach { field ->
        val constant = field.jniConstant
        if (constant != null) {
          val fieldStub = field.name.toJniFieldStub()
          println("#undef ${cName}_$fieldStub")
          println("#define ${cName}_$fieldStub $constant")
          println()
        }
      }
  }
}

/**
 * Writes JNI function declarations for native methods to the JNI header.
 *
 * This function generates C/C++ function declarations following the JNI convention:
 * - Creates a comment block with class, method, and signature information
 * - Outputs the JNIEXPORT and JNICALL declarations with appropriate return type
 * - Generates the JNI-mangled function name (handling overloaded methods)
 * - Adds parameter lists with appropriate JNI types
 *
 * ###### Example
 * ```c
 * /*
 *  * Class:     com_example_MyClass
 *  * Method:    doSomething
 *  * Signature: (ILjava/lang/String;)V
 *  */
 * JNIEXPORT void JNICALL Java_com_example_MyClass_doSomething
 *   (JNIEnv *, jobject, jint, jstring);
 * ```
 *
 * @param cName The JNI-mangled class name for comments
 * @param classFlatName The fully qualified class name with dots
 * @param nativeMethods The list of native methods to process
 * @param overloadedMethodMap A map of method names to their occurrence count for detecting overloaded methods
 */
internal fun PrintWriter.writeNativeMethods(
  cName: String,
  classFlatName: String,
  nativeMethods: List<MethodNode>,
  overloadedMethodMap: Map<String, Int>,
) {
  for (method in nativeMethods) {
    val isOverloaded = overloadedMethodMap[method.name].orZero() > 1
    // Method comment block
    println("/*")
    println(" * Class:     $cName")
    println(" * Method:    ${method.name.toJniFieldStub()}")
    println(" * Signature: ${method.desc}")
    println(" */")

    // Return type and method name
    println("JNIEXPORT ${Type.getReturnType(method.desc).jniType} JNICALL ${method.toMangledJniMethod(classFlatName, isOverloaded)}")
    print("  (JNIEnv *, ")
    print(if (method.isStatic) "jclass" else "jobject")

    // Parameter types
    val paramTypes = Type.getArgumentTypes(method.desc)
    // Need to check `isNotEmpty` to make sure we don't add a comma when there are no params.
    if (paramTypes.isNotEmpty()) {
      print(paramTypes.joinToString(prefix = ", ", separator = ", ") { it.jniType })
    }

    // End of method
    println(");")
    println()
  }
}

/**
 * Converts a Java class name to its JNI representation.
 *
 * This function follows the JNI class name mangling rules for header files:
 * - Alphanumeric characters are preserved as-is
 * - Dots and underscores are converted to single underscores '_'
 * - Dollar signs (used for inner classes) are converted to double underscores '__'
 * - Other special characters are encoded using their hex representation
 *
 * Example: "java.lang.String" becomes "java_lang_String"
 * Example: "com.example.Outer$Inner" becomes "com_example_Outer__Inner"
 *
 * @return The JNI-compatible class name string
 */
internal fun String.toMangledJniName(): String = buildString(100) {
  for (c in this@toMangledJniName) {
    when {
      c.isAsciiAlphanumeric() -> append(c)
      c == '.' || c == '_' -> append("_")
      c == '$' -> append("__")
      else -> append(c.toJniSymbol())
    }
  }
}

/**
 * Converts a string to a JNI identifier according to JNI method naming rules.
 *
 * Used primarily for method names in JNI function declarations:
 * - Alphanumeric characters are preserved as-is
 * - Forward slashes and dots are converted to underscores '_'
 * - Underscores are encoded as "_1"
 * - Semicolons are encoded as "_2"
 * - Square brackets are encoded as "_3"
 * - Other special characters are encoded using their hex representation
 *
 * Example: "getValue" remains "getValue"
 * Example: "some/path" becomes "some_path"
 * Example: "has_underscore" becomes "has_1underscore"
 *
 * @return The JNI-compatible identifier string
 */
internal fun String.toJniIdentifier(): String = buildString(100) {
  for (c in this@toJniIdentifier) {
    when {
      c.isAsciiAlphanumeric() -> append(c)
      c == '/' || c == '.' -> append("_")
      c == '_' -> append("_1")
      c == ';' -> append("_2")
      c == '[' -> append("_3")
      else -> append(c.toJniSymbol())
    }
  }
}

/**
 * Converts a field name to its JNI field stub representation.
 *
 * Used for field identifiers in JNI #define directives:
 * - Alphanumeric characters and underscores are preserved as-is
 * - All other characters are encoded using their hex representation
 *
 * This encoding is used in JNI headers for representing field names in macro definitions for constants.
 *
 * Example: "VERSION" remains "VERSION"
 * Example: "has_underscores" remains "has_underscores"
 * Example: "special@chars" becomes "special_00040chars"
 *
 * @return The JNI-compatible field stub string
 */
internal fun String.toJniFieldStub(): String = buildString(100) {
  for (c in this@toJniFieldStub) {
    when {
      c.isAsciiAlphanumeric() || c == '_' -> append(c)
      else -> append(c.toJniSymbol())
    }
  }
}

internal fun MethodNode.toMangledJniMethod(classFlatName: String, isOverloaded: Boolean): String = buildString(100) {
  append("Java_")
  append(classFlatName.toJniIdentifier())
  append('_')
  append(name.toJniIdentifier())
  if (isOverloaded) {
    // We need to encode the parameter types for overloaded methods.
    val methodType = Type.getMethodType(desc)
    val paramTypes = methodType.argumentTypes
    // Even if there are no arguments, we always append double underscore for overloaded methods.
    append("__")
    paramTypes.forEach { append(it.descriptor.toJniIdentifier()) }
  }
}

internal inline fun Char.toJniSymbol(): String {
  val hexString = Integer.toHexString(this.code)
  val zerosToPad = 5 - hexString.length
  val result = CharArray(6)
  result[0] = '_'
  for (i in 1..zerosToPad) {
    result[i] = '0'
  }
  for (i in zerosToPad + 1 until 6) {
    result[i] = hexString[i - zerosToPad - 1]
  }
  return String(result)
}

// This method leaves a lot to be desired and there are likely much better alternatives.
// It's also the likely culprit for issues that result from this plugin, at least issues related to the contents of the header files.
private fun buildClassHierarchy(classNode: ClassNode, srcDir: File): List<ClassNode> {
  val hierarchy = mutableListOf<ClassNode>()
  var currentNode = classNode

  // Add the current class first so that it is last when the list is reversed.
  hierarchy.add(currentNode)
  for (innerClass in classNode.innerClasses) {
    // We might incorrectly load unrelated classes that happen to be referenced in the innerClasses list, potentially including constants
    // from classes that have no relationship to our target class, so verify the names match.
    if (innerClass.name == classNode.name && innerClass.outerName != null) {
      val outerClassName = innerClass.outerName
      val outerClassFile = srcDir.resolve("${outerClassName.replace('/', File.separatorChar)}.class")
      if (outerClassFile.exists()) {
        val outerClassNode = FileInputStream(outerClassFile).use { inputStream ->
          val reader = ClassReader(inputStream)
          val node = ClassNode()
          reader.accept(node, 0)
          node
        }
        hierarchy.add(outerClassNode)
      }
    }
  }

  // Reverse to get super-class first order.
  return hierarchy.reversed()
}
