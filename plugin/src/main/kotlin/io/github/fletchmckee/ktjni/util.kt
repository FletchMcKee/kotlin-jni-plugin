// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("NOTHING_TO_INLINE")

package io.github.fletchmckee.ktjni

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

/**
 * Windows encodes longs as "i64" instead of "LL"
 */
internal val isWindows: Boolean = System.getProperty("os.name").startsWith("Windows")

/**
 * Heuristic for detecting local/anonymous classes (JNI headers should not be generated for these classes).
 *
 * This checks whether the class node has a non-null `outerMethod`, which is commonly true or local classes (classes defined inside a
 * method or lambda).
 *
 * This is not foolproof. ASM doesn't provide a direct way to check if a class is local or anonymous. This mimics the behavior of javac's
 * `isDirectlyOrIndirectlyLocal()`.
 */
internal val ClassNode.isLocal: Boolean get() = outerMethod != null

/**
 * When this is true and [isSynthetic] is false, we're going to generate headers.
 */
internal val MethodNode.isNative: Boolean get() = (access and Opcodes.ACC_NATIVE) != 0

/**
 * Determines if a method is synthetic (compiler-generated).
 * Synthetic methods do not need JNI headers as they are not explicitly declared in the source code.
 */
internal val MethodNode.isSynthetic: Boolean get() = (access and Opcodes.ACC_SYNTHETIC) != 0

/**
 * When [isLocal] is false and this is true, we're going to generate headers.
 */
internal val MethodNode.needsHeader: Boolean get() = isNative && !isSynthetic

/**
 * Checks if a method is declared as static.
 * Static methods have different JNI function signatures than instance methods (jclass instead of jobject).
 */
internal val MethodNode.isStatic: Boolean get() = (access and Opcodes.ACC_STATIC) != 0

/**
 * Static final fields are added to headers with native methods.
 */
internal val FieldNode.isStaticFinal: Boolean
  get() = (access and (Opcodes.ACC_STATIC or Opcodes.ACC_FINAL)) == (Opcodes.ACC_STATIC or Opcodes.ACC_FINAL)

/**
 * Maps a JVM type (ASM's [Type]) to its corresponding JNI C type.
 *
 * This is used when generating method parameter and return types for `JNIEXPORT` function
 * declarations in C/C++ header files.
 *
 * ###### Example
 * The `Int` return type from this method...
 * ```kotlin
 * external fun nativeMethod(): Int
 * ```
 * maps as `jint` for the header.
 * ```c
 * JNIEXPORT jint JNICALL ...
 * ```
 */
internal val Type.jniType: String get() = when (this.sort) {
  Type.VOID -> "void"
  Type.BOOLEAN -> "jboolean"
  Type.BYTE -> "jbyte"
  Type.CHAR -> "jchar"
  Type.SHORT -> "jshort"
  Type.INT -> "jint"
  Type.LONG -> "jlong"
  Type.FLOAT -> "jfloat"
  Type.DOUBLE -> "jdouble"
  Type.ARRAY -> {
    when (elementType.sort) {
      Type.BOOLEAN -> "jbooleanArray"
      Type.BYTE -> "jbyteArray"
      Type.CHAR -> "jcharArray"
      Type.SHORT -> "jshortArray"
      Type.INT -> "jintArray"
      Type.LONG -> "jlongArray"
      Type.FLOAT -> "jfloatArray"
      Type.DOUBLE -> "jdoubleArray"
      Type.ARRAY,
      Type.OBJECT,
      -> "jobjectArray"
      else -> throw IllegalArgumentException("Unknown array component type: $elementType")
    }
  }
  Type.OBJECT -> {
    when (internalName) {
      "java/lang/String" -> "jstring"
      "java/lang/Class" -> "jclass"
      "java/lang/Throwable",
      "java/lang/Exception",
      "java/lang/Error",
      -> "jthrowable"
      else -> "jobject"
    }
  }
  else -> throw IllegalArgumentException("Unknown type: $this")
}

/**
 * Converts a static final field's value into a valid C preprocessor constant
 * according to JNI header rules.
 *
 * This is used for generating `#define` statements for constants in header files.
 * It handles Java primitive values, Strings, and encodes them in a JNI-compatible way:
 *
 * ###### Example
 * This java static final
 * ```kotlin
 * package com.example
 *
 * class MyClass {
 *   companion object {
 *     const val BUILD_ID = 123456789L
 *   }
 *   external fun nativeMethod()
 * }
 * ```
 * appends `i64` for Windows...
 * ```c
 * #undef com_example_MyClass_BUILD_ID
 * #define com_example_MyClass_BUILD_ID 123456789i64
 * ```
 * and `LL` for other operating systems
 * ```c
 * #undef com_example_MyClass_BUILD_ID
 * #define com_example_MyClass_BUILD_ID 123456789LL
 * ```
 */
internal val FieldNode.jniConstant: String?
  get() {
    val value = this.value ?: return null

    return when (desc) {
      "Z" -> {
        // For boolean fields, ASM provides the value as Int (0 or 1)
        when (value) {
          is Int -> if (value.toInt() != 0) "1L" else "0L"
          else -> null
        }
      }
      "B", // Byte
      "S", // Short
      "I", // Int
      -> "$value" + "L"
      "J" -> "$value" + if (isWindows) "i64" else "LL" // Long
      "C" -> "${(value as? Int)?.toInt()?.and(0xffff)}L" // Char (as integer code point)
      "F" -> { // Float
        val fv = value as Float
        if (fv.isInfinite()) {
          (if (fv < 0) "-" else "") + "Inff"
        } else {
          "$value" + "f"
        }
      }
      "D" -> { // Double
        val d = value as Double
        if (d.isInfinite()) {
          (if (d < 0) "-" else "") + "InfD"
        } else {
          "$value"
        }
      }
      "Ljava/lang/String;" -> "\"$value\"" // String
      else -> null // Other types not supported
    }
  }

internal inline fun Char.isAsciiAlphanumeric(): Boolean = this.code <= 0x7f && this.isLetterOrDigit()

/**
 * Simple helper extension that makes it easier to read when dealing with nulls.
 */
internal inline fun Int?.orZero(): Int = this ?: 0
