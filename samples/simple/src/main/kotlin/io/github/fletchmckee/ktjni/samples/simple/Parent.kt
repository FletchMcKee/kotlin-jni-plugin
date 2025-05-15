// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni.samples.simple

class Parent {
  companion object {
    const val VERSION = 1
  }

  external fun parentJniMethod()

  inner class Child {
    external fun childJniMethod()
  }
}
