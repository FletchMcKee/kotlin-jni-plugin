// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.objectweb.asm.tree.MethodNode

class KtjniWriterTest {
  @Test fun `toMangledJniName - handles special characters correctly`() {
    assertThat("java.lang.String".toMangledJniName()).isEqualTo("java_lang_String")
    assertThat("com.example.Class\$Inner".toMangledJniName()).isEqualTo("com_example_Class__Inner")
    assertThat("com.example.Class+".toMangledJniName()).isEqualTo("com_example_Class_0002b")
    assertThat("com_example_Class".toMangledJniName()).isEqualTo("com_example_Class")
    assertThat("com\$example\$Class".toMangledJniName()).isEqualTo("com__example__Class")
    assertThat("weird@class#name".toMangledJniName()).isEqualTo("weird_00040class_00023name")
  }

  @Test fun `toJniIdentifier - encodes method names correctly`() {
    // Basic cases
    assertThat("getName".toJniIdentifier()).isEqualTo("getName")
    assertThat("com/example/method".toJniIdentifier()).isEqualTo("com_example_method")

    // Special character handling
    assertThat("process_data".toJniIdentifier()).isEqualTo("process_1data")
    assertThat("handle;input".toJniIdentifier()).isEqualTo("handle_2input")
    assertThat("process[array]".toJniIdentifier()).isEqualTo("process_3array_0005d")
    assertThat("weird@method".toJniIdentifier()).isEqualTo("weird_00040method")
  }

  @Test fun `toJniFieldStub - preserves alphanumeric and underscores`() {
    assertThat("CONSTANT_VALUE".toJniFieldStub()).isEqualTo("CONSTANT_VALUE")
    assertThat("has_multiple_underscores".toJniFieldStub()).isEqualTo("has_multiple_underscores")
    assertThat("special@char#field".toJniFieldStub()).isEqualTo("special_00040char_00023field")
    assertThat("123numeric".toJniFieldStub()).isEqualTo("123numeric")
  }

  @Test fun `toJniSymbol - converts characters to hex representation`() {
    assertThat('@'.toJniSymbol()).isEqualTo("_00040")
    assertThat('#'.toJniSymbol()).isEqualTo("_00023")
    assertThat('+'.toJniSymbol()).isEqualTo("_0002b")
    assertThat('λ'.toJniSymbol()).isEqualTo("_003bb")
  }

  @Test fun `toMangledJniMethod - handles method overloading`() {
    val fakeMethodNode = fakeMethodNode("getValue", "(I)V")

    // Verify non-overloaded method
    assertThat(fakeMethodNode.toMangledJniMethod("com.example.TestClass", false))
      .isEqualTo("Java_com_example_TestClass_getValue")

    // Verify overloaded method
    assertThat(fakeMethodNode.toMangledJniMethod("com.example.TestClass", true))
      .isEqualTo("Java_com_example_TestClass_getValue__I")

    // Verify special characters
    val specialMethodNode = fakeMethodNode("process_data", "(Ljava/lang/String;)Z")
    assertThat(specialMethodNode.toMangledJniMethod("com.example.TestClass", true))
      .isEqualTo("Java_com_example_TestClass_process_1data__java_lang_String")
  }

  private fun fakeMethodNode(name: String, descriptor: String): MethodNode = MethodNode().apply {
    this.name = name
    this.desc = descriptor
  }
}
