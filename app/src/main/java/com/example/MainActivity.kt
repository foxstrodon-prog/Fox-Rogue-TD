package com.example

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.RogueTDScreen
import com.example.ui.RogueTDViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  // The ViewModel owns the whole match, so rotation / resize never resets the run.
  private val viewModel: RogueTDViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(dynamicColor = false) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = Color(0xFF070B13)
        ) {
          RogueTDScreen(viewModel = viewModel)
        }
      }
    }
  }

  /**
   * The manifest declares configChanges for orientation/size/density, so the activity is not
   * recreated on rotation. Compose observes the new window size on its own (BoxWithConstraints);
   * here we only re-apply the edge-to-edge system bar styling for the new orientation.
   */
  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    enableEdgeToEdge()
  }
}
