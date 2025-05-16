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

  @Test fun `toMangledJniMethod - non-overloaded method with no arguments`() {
    val methodNode = fakeMethodNode("nativeMethod", "()V")
    assertThat(methodNode.toMangledJniMethod("com.example.TestClass", false))
      .isEqualTo("Java_com_example_TestClass_nativeMethod")
  }

  @Test fun `toMangledJniMethod - overloaded method with no arguments`() {
    val methodNode = fakeMethodNode("nativeMethod", "()V")
    assertThat(methodNode.toMangledJniMethod("com.example.TestClass", true))
      .isEqualTo("Java_com_example_TestClass_nativeMethod__")
  }

  @Test fun `toMangledJniMethod - overloaded method with primitive arguments`() {
    val methodNode = fakeMethodNode("nativeMethod", "(I)V")
    assertThat(methodNode.toMangledJniMethod("com.example.TestClass", true))
      .isEqualTo("Java_com_example_TestClass_nativeMethod__I")
  }

  @Test fun `toMangledJniMethod - overloaded method with object argument`() {
    val methodNode = fakeMethodNode("nativeMethod", "(Ljava/lang/String;)V")
    assertThat(methodNode.toMangledJniMethod("com.example.TestClass", true))
      .isEqualTo("Java_com_example_TestClass_nativeMethod__Ljava_lang_String_2")
  }

  @Test fun `test overloaded method with array arguments`() {
    val methodNode = fakeMethodNode("nativeMethod", "([I[F)V")
    assertThat(methodNode.toMangledJniMethod("com.example.TestClass", true))
      .isEqualTo("Java_com_example_TestClass_nativeMethod___3I_3F")
  }

  @Test fun `test overloaded method with multidimensional arrays`() {
    val methodNode = fakeMethodNode("nativeMethod", "([[I[[[F)V")
    assertThat(methodNode.toMangledJniMethod("com.example.TestClass", true))
      .isEqualTo("Java_com_example_TestClass_nativeMethod___3_3I_3_3_3F")
  }

  @Test fun `test overloaded method with object arrays`() {
    val methodNode = fakeMethodNode("nativeMethod", "([Ljava/lang/String;)V")
    assertThat(methodNode.toMangledJniMethod("com.example.TestClass", true))
      .isEqualTo("Java_com_example_TestClass_nativeMethod___3Ljava_lang_String_2")
  }

  @Test fun `toMangledJniMethod - overloaded method with special characters`() {
    val methodNode = fakeMethodNode("process_data", "(Ljava/lang/String;)Z")
    assertThat(methodNode.toMangledJniMethod("com.example.TestClass", true))
      .isEqualTo("Java_com_example_TestClass_process_1data__Ljava_lang_String_2")
  }

  @Test fun `toMangledJniName - complex mixed argument types`() {
    val methodNode = fakeMethodNode("nativeMethod", "(I[FJLjava/lang/String;[[Z)V")
    assertThat(methodNode.toMangledJniMethod("com.example.TestClass", true))
      .isEqualTo("Java_com_example_TestClass_nativeMethod__I_3FJLjava_lang_String_2_3_3Z")
  }

  private fun fakeMethodNode(name: String, descriptor: String): MethodNode = MethodNode().apply {
    this.name = name
    this.desc = descriptor
  }
}
