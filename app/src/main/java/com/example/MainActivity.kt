package com.example

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface as ComposeSurface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.model.GameSpeed
import com.example.ui.RogueTDScreen
import com.example.ui.RogueTDViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  // The ViewModel owns the whole match, so rotation / resize never resets the run.
  val viewModel: RogueTDViewModel by viewModels()

  private var orientationEventListener: OrientationEventListener? = null
  private var lastOrientBucket: Float = 0f

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Screen keep-awake management tied directly to active game loop state
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          val isPlaying = state.gameSpeed != GameSpeed.PAUSED &&
                  !state.isPauseMenuOpen &&
                  !state.isGameOver &&
                  !state.isVictory
          if (isPlaying) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
          } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
          }
        }
      }
    }

    orientationEventListener = object : OrientationEventListener(this) {
      override fun onOrientationChanged(orientation: Int) {
        if (orientation == ORIENTATION_UNKNOWN) return
        val displayRotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          display?.rotation ?: Surface.ROTATION_0
        } else {
          @Suppress("DEPRECATION")
          windowManager.defaultDisplay.rotation
        }
        val displayAngle = when (displayRotation) {
          Surface.ROTATION_90 -> 90f
          Surface.ROTATION_180 -> 180f
          Surface.ROTATION_270 -> 270f
          else -> 0f
        }
        val rawAngle = computeOrientationBucket(orientation, lastOrientBucket)
        lastOrientBucket = rawAngle
        val relativeAngle = (rawAngle - displayAngle + 360f) % 360f
        viewModel.updateDeviceRotation(relativeAngle)
      }
    }
    setContent {
      MyApplicationTheme(dynamicColor = false) {
        ComposeSurface(
          modifier = Modifier.fillMaxSize(),
          color = Color(0xFF070B13)
        ) {
          RogueTDScreen(viewModel = viewModel)
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    if (orientationEventListener?.canDetectOrientation() == true) {
      orientationEventListener?.enable()
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.soundManager.resumeAudio()
  }

  override fun onPause() {
    super.onPause()
    // Instantly terminate all audio on focus/lifecycle transition (TASK 1)
    viewModel.soundManager.suspendAllAudio()
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    // Cut all audio playback immediately when app transitions out of active focus
    // (Do not suspend if focus shifted to an in-app Compose Dialog such as Pause Menu)
    if (!hasFocus) {
      if (!viewModel.isInternalDialogActive()) {
        viewModel.soundManager.suspendAllAudio()
      }
    } else {
      viewModel.soundManager.resumeAudio()
    }
  }

  override fun onStop() {
    super.onStop()
    orientationEventListener?.disable()
    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    viewModel.openPauseMenu()
    viewModel.soundManager.suspendAllAudio()
  }

  override fun onDestroy() {
    super.onDestroy()
    orientationEventListener?.disable()
    orientationEventListener = null
    viewModel.soundManager.suspendAllAudio()
  }

  /**
   * The manifest declares configChanges for orientation/size/density, so the activity is not
   * recreated on rotation. Compose observes the new window size on its own (BoxWithConstraints);
   * here we only re-apply the edge-to-edge system bar styling for the new orientation.
   */
  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    enableEdgeToEdge()
    if (orientationEventListener == null || !orientationEventListener!!.canDetectOrientation()) {
      viewModel.updateDeviceRotation(0f)
    }
  }

  companion object {
    fun computeOrientationBucket(orientation: Int, currentBucket: Float, deadband: Int = 15): Float {
      if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return currentBucket
      val halfGap = 45 - deadband
      return when {
        orientation in (90 - halfGap)..(90 + halfGap) -> 90f
        orientation in (180 - halfGap)..(180 + halfGap) -> 180f
        orientation in (270 - halfGap)..(270 + halfGap) -> 270f
        orientation in 0..halfGap || orientation in (360 - halfGap)..359 -> 0f
        else -> currentBucket
      }
    }
  }
}
