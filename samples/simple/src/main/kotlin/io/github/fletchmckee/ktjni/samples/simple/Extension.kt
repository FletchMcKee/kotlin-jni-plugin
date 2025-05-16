// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni.samples.simple

class Extension {
  external fun nativeExtension(): Int
  external fun nativeExtension(array1: FloatArray, array2: FloatArray): Float
  companion object {
    @JvmStatic
    external fun String.nativeExtension(): Int

    @JvmStatic
    external fun String.nativeExtensionV2(): Int
  }

  inner class SubExtension {
    external fun anotherTest(someInt: Int = 9): Int
  }
}
