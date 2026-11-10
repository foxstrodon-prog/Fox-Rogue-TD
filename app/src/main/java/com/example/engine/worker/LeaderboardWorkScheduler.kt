package com.example.engine.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class LeaderboardWorkScheduler(private val context: Context) {

  companion object {
    const val WORK_NAME = "score_submission_work"
  }

  fun triggerImmediateSync() {
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    val request = OneTimeWorkRequestBuilder<ScoreSubmissionWorker>()
      .setConstraints(constraints)
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
      .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
      WORK_NAME,
      ExistingWorkPolicy.APPEND_OR_REPLACE,
      request
    )
  }
}
