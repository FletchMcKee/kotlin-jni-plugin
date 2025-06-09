// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.FieldNode

class WriterUtilsTest {
  @Test fun `isAsciiAlphanumeric - returns true for ASCII chars`() {
    val chars = listOf('A', 'Z', 'a', 'z', '0', '9')
    for (char in chars) {
      assertThat(char.isAsciiAlphanumeric()).isTrue()
    }
  }

  @Test fun `isAsciiAlphanumeric - returns false for non-ASCII characters`() {
    val chars = listOf('ñ', 'ø', 'λ', 'Ж', '中')
    for (char in chars) {
      assertThat(char.isAsciiAlphanumeric()).isFalse()
    }
  }

  @Test fun `isAsciiAlphanumeric - returns false for ASCII non-alphanumeric characters`() {
    val chars = listOf('!', '@', '[', '`', '~', ' ')
    for (char in chars) {
      assertThat(char.isAsciiAlphanumeric()).isFalse()
    }
  }

  @Test fun `isAsciiAlphanumeric - returns false for control characters`() {
    val chars = listOf('\u0000', '\u001F', '\u007F') // NULL, Unit Separator, DEL
    for (char in chars) {
      assertThat(char.isAsciiAlphanumeric()).isFalse()
    }
  }

  @Test fun `jniConstant - true maps to 1L`() {
    val node = createFieldNodeConst("Z", 1)
    assertThat(node.jniConstant).isEqualTo("1L")
  }

  @Test fun `jniConstant - false maps to 0L`() {
    val node = createFieldNodeConst("Z", 0)
    assertThat(node.jniConstant).isEqualTo("0L")
  }

  @Test fun `jniConstant - int constant maps to L`() {
    val jniConstant = createFieldNodeConst("I", 42).jniConstant
    assertThat(jniConstant).isEqualTo("42L")
  }

  @Test fun `jniConstant - byte constant maps to L`() {
    val node = createFieldNodeConst("B", 127) // Max byte value
    assertThat(node.jniConstant).isEqualTo("127L")
  }

  @Test fun `jniConstant - short constant maps to L`() {
    val node = createFieldNodeConst("S", 32767) // Max short value
    assertThat(node.jniConstant).isEqualTo("32767L")
  }

  @Test fun `jniConstant - char constant is masked and mapped to L`() {
    val node = createFieldNodeConst("C", 'A'.code)
    assertThat(node.jniConstant).isEqualTo("65L")
  }

  @Test fun `jniConstant - char min value maps to L`() {
    val node = createFieldNodeConst("C", Char.MIN_VALUE.code)
    assertThat(node.jniConstant).isEqualTo("0L")
  }

  @Test fun `jniConstant - char max value maps to L`() {
    val node = createFieldNodeConst("C", Char.MAX_VALUE.code)
    assertThat(node.jniConstant).isEqualTo("65535L")
  }

  @Test fun `jniConstant - char ascii upper bound`() {
    val node = createFieldNodeConst("C", 0x7F) // '\u007F'
    assertThat(node.jniConstant).isEqualTo("127L")
  }

  @Test fun `jniConstant - char high surrogate`() {
    val node = createFieldNodeConst("C", 0xD800)
    assertThat(node.jniConstant).isEqualTo("55296L")
  }

  @Test fun `jniConstant - special chars map correctly`() {
    val node = createFieldNodeConst("C", 'ñ'.code)
    assertThat(node.jniConstant).isEqualTo("${'ñ'.code}L")
  }

  @Test fun `jniConstant - char with value above 0xFFFF is masked`() {
    val node = createFieldNodeConst("C", 0x1FFFF) // 17 bits set
    val masked = 0x1FFFF and 0xFFFF
    assertThat(node.jniConstant).isEqualTo("${masked}L")
  }

  @Test fun `jniConstant - long constant appends LL or i64 depending on OS`() {
    val node = createFieldNodeConst("J", 123456789L)
    when {
      isWindows -> assertThat(node.jniConstant).isEqualTo("123456789i64")
      else -> assertThat(node.jniConstant).isEqualTo("123456789LL")
    }
  }

  @Test fun `jniConstant - float constant maps to f`() {
    val node = createFieldNodeConst("F", 3.14f).jniConstant
    assertThat(node).isEqualTo("3.14f")
  }

  @Test fun `jniConstant - infinite float handled correctly`() {
    val positiveJniConstant = createFieldNodeConst("F", Float.POSITIVE_INFINITY).jniConstant
    val negativeJniConstant = createFieldNodeConst("F", Float.NEGATIVE_INFINITY).jniConstant
    assertThat(positiveJniConstant).isEqualTo("Inff")
    assertThat(negativeJniConstant).isEqualTo("-Inff")
  }

  @Test fun `jniConstant - double constant maps to string`() {
    val node = createFieldNodeConst("D", 2.718).jniConstant
    assertThat(node).isEqualTo("2.718")
  }

  @Test fun `jniConstant - infinite double handled correctly`() {
    val positiveNode = createFieldNodeConst("D", Double.POSITIVE_INFINITY).jniConstant
    val negativeNode = createFieldNodeConst("D", Double.NEGATIVE_INFINITY).jniConstant
    assertThat(positiveNode).isEqualTo("InfD")
    assertThat(negativeNode).isEqualTo("-InfD")
  }

  @Test fun `jniConstant - string constant is quoted`() {
    val node = createFieldNodeConst("Ljava/lang/String;", "hello").jniConstant
    assertThat(node).isEqualTo("\"hello\"")
  }

  @Test fun `jniConstant - null value returns null`() {
    val node = createFieldNodeConst("I", null).jniConstant
    assertThat(node).isNull()
  }

  @Test fun `jniConstant - unsupported type returns null`() {
    val node = createFieldNodeConst("Ljava/lang/Object;", Any()).jniConstant
    assertThat(node).isNull()
  }

  private fun createFieldNodeConst(desc: String, value: Any?): FieldNode = FieldNode(
    Opcodes.ASM9,
    Opcodes.ACC_STATIC or Opcodes.ACC_FINAL,
    "CONST",
    desc,
    null, // signature (not needed)
    value,
  )
}
