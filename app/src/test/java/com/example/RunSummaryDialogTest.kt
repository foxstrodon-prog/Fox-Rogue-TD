package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.model.GlobalBoardStatus
import com.example.model.RunSubmissionState
import com.example.ui.GameUIState
import com.example.ui.dialog.RunSummarySubmissionBanner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w480dp-h900dp")
class RunSummaryDialogTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun `SUBMITTED state with older pending queue items shows submitted banner not uploading`() {
    composeRule.setContent {
      RunSummarySubmissionBanner(
        state = GameUIState(
          runSubmissionState = RunSubmissionState.SUBMITTED,
          leaderboardPendingCount = 2,
          leaderboardStatus = GlobalBoardStatus.ONLINE
        )
      )
    }

    composeRule.onNodeWithText("SCORE SUBMITTED TO GLOBAL LEADERBOARD").assertIsDisplayed()
    composeRule.onNodeWithText("UPLOADING RUN TO GLOBAL RANKS...").assertDoesNotExist()
  }

  @Test
  fun `submission error is shown even when leaderboardStatus is NOT_CONFIGURED`() {
    composeRule.setContent {
      RunSummarySubmissionBanner(
        state = GameUIState(
          runSubmissionState = RunSubmissionState.NONE,
          leaderboardStatus = GlobalBoardStatus.NOT_CONFIGURED,
          leaderboardSubmissionError = "Rejected by rules"
        )
      )
    }

    composeRule.onNodeWithText("UPLOAD FAILED: Rejected by rules").assertIsDisplayed()
    composeRule.onNodeWithText("LOCAL ONLY (GLOBAL BOARD UNCONFIGURED)").assertDoesNotExist()
  }

  @Test
  fun `LOCAL_ONLY state displays local only banner`() {
    composeRule.setContent {
      RunSummarySubmissionBanner(
        state = GameUIState(
          runSubmissionState = RunSubmissionState.LOCAL_ONLY,
          leaderboardSubmissionError = "Backend key missing"
        )
      )
    }

    composeRule.onNodeWithText("Backend key missing").assertIsDisplayed()
  }

  @Test
  fun `PENDING state displays uploading banner`() {
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      RunSummarySubmissionBanner(
        state = GameUIState(
          runSubmissionState = RunSubmissionState.PENDING,
          leaderboardPendingCount = 1,
          leaderboardStatus = GlobalBoardStatus.ONLINE
        )
      )
    }
    composeRule.mainClock.advanceTimeByFrame()

    composeRule.onNodeWithText("UPLOADING RUN TO GLOBAL RANKS...").assertIsDisplayed()
  }
}
