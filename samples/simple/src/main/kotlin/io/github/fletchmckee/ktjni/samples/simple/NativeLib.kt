// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni.samples.simple

class NativeLib {
  external fun stringFromJni(): String
  external fun longFromJni(): Long
  external fun longFromJni(value: Long): Long

  companion object {
    // Various types of constants to test JNI static defines
    const val VERSION = 42
    const val PI = 3.14159
    const val ENABLED = true
    const val RATIO = 2.5f
    const val MAX_VALUE = 9223372036854775807L
    const val NAME = "NativeLib"
    const val X_CHAR = 'X'
    const val MIN_BYTE: Byte = -128
    const val MAX_SHORT: Short = 32767

    // Constants with special values
    const val POS_INFINITY = Float.POSITIVE_INFINITY
    const val NEG_INFINITY = Double.NEGATIVE_INFINITY

    // Not a constant - verify it isn't in the JNI header
    val runtimeValue = System.currentTimeMillis()

    init {
      System.loadLibrary("simple")
    }
  }
}
