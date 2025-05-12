// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni.samples.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.fletchmckee.ktjni.samples.demo.ui.theme.KtjniTheme
import io.github.fletchmckee.ktjni.samples.simple.NativeLib

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      KtjniTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Greeting(
            greeting = NativeLib().stringFromJNI(),
            modifier = Modifier.padding(innerPadding),
          )
        }
      }
    }
  }
}

@Composable
fun Greeting(greeting: String, modifier: Modifier = Modifier) {
  Text(
    text = greeting,
    modifier = modifier,
  )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  KtjniTheme {
    Greeting("Android")
  }
}
