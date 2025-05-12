// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni.samples.simple

class NativeLib {
  external fun stringFromJNI(): String

  companion object {
    // Used to load the 'simple' library on application startup.
    init {
      System.loadLibrary("simple")
    }
  }
}
