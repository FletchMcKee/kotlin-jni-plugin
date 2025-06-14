// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni.samples.demo

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.fletchmckee.ktjni.samples.demo.ui.theme.KtjniTheme
import io.github.fletchmckee.ktjni.samples.simple.gaussianBlur
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContent {
      KtjniTheme {
        Scaffold(
          modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues()),
        ) { innerPadding ->
          BlurDemo(Modifier.padding(innerPadding))
        }
      }
    }
  }

  init {
    System.loadLibrary("simple")
  }
}

@Composable
fun BlurDemo(modifier: Modifier = Modifier) {
  val context = LocalContext.current

  // Decode original once and cache it
  val originalBitmap = remember {
    BitmapFactory.decodeResource(context.resources, R.drawable.moon_and_stars)!!
  }

  var sliderSigma by remember { mutableFloatStateOf(0f) }
  var sigma by remember { mutableFloatStateOf(0f) }
  var blurredImage by remember { mutableStateOf<ImageBitmap?>(null) }

  LaunchedEffect(sigma, originalBitmap) {
    val result = withContext(Dispatchers.Default) {
      originalBitmap.gaussianBlur(sigma).asImageBitmap()
    }
    blurredImage = result
  }

  Column(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = modifier.padding(16.dp),
  ) {
    Text("Gaussian Blur Sigma: ${sigma.roundToInt()}")

    BlurrableImage(
      blurredImage = blurredImage,
      modifier = Modifier.aspectRatio(1f)
        .clip(RoundedCornerShape(2))
        .background(Color.LightGray),
    )

    Slider(
      value = sliderSigma,
      onValueChange = { sliderSigma = it },
      onValueChangeFinished = { sigma = sliderSigma },
      valueRange = 0f..100f,
      steps = 25,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp),
    )
  }
}

@Composable
fun BlurrableImage(
  blurredImage: ImageBitmap?,
  modifier: Modifier = Modifier,
) {
  Box(modifier) {
    androidx.compose.animation.AnimatedVisibility(
      visible = blurredImage != null,
      modifier = Modifier.fillMaxSize(),
    ) {
      blurredImage?.let {
        Image(bitmap = it, contentDescription = "Blurred image")
      }
    }
  }
}
