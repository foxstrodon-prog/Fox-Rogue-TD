package com.example.engine

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

data class UpdateInfo(
    val isUpdateAvailable: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val releaseTitle: String = "",
    val releaseNotes: String = "",
    val apkUrl: String? = null,
    val apkFileName: String? = null
)

enum class UpdateDownloadStatus {
    IDLE,
    CHECKING,
    AVAILABLE,
    DOWNLOADING,
    READY_TO_INSTALL,
    FAILED,
    UP_TO_DATE
}

data class AppUpdateState(
    val status: UpdateDownloadStatus = UpdateDownloadStatus.IDLE,
    val updateInfo: UpdateInfo? = null,
    val progressPercent: Int = 0,
    val downloadedApkFile: File? = null,
    val errorMessage: String? = null
)

class AppUpdateManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    companion object {
        const val GITHUB_REPO: String = "foxstrodon-prog/Fox-Rogue-TD"
        private const val API_URL: String = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .build()
    }

    private val _updateState = MutableStateFlow(AppUpdateState())
    val updateState: StateFlow<AppUpdateState> = _updateState.asStateFlow()

    private var activeDownloadId: Long = -1L
    private var progressPollingJob: Job? = null
    private var downloadCompleteReceiver: BroadcastReceiver? = null

    init {
        registerDownloadReceiver()
    }

    private fun registerDownloadReceiver() {
        if (downloadCompleteReceiver != null) return

        downloadCompleteReceiver = object : BroadcastReceiver() {
            override fun onReceive(recvContext: Context?, intent: Intent?) {
                if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id != -1L && id == activeDownloadId) {
                        checkDownloadStatus(id)
                    }
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            context,
            downloadCompleteReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    fun unregisterReceiver() {
        try {
            downloadCompleteReceiver?.let {
                context.unregisterReceiver(it)
                downloadCompleteReceiver = null
            }
        } catch (_: Exception) {}
        progressPollingJob?.cancel()
    }

    /**
     * Queries GitHub Releases API for the latest version and inspects assets for an APK.
     */
    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        _updateState.update { it.copy(status = UpdateDownloadStatus.CHECKING, errorMessage = null) }
        try {
            val request = Request.Builder()
                .url(API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "RogueTD-DualFront-App/${BuildConfig.VERSION_NAME}")
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorMsg = "GitHub API error: HTTP ${response.code}"
                _updateState.update { it.copy(status = UpdateDownloadStatus.FAILED, errorMessage = errorMsg) }
                return@withContext UpdateInfo(
                    isUpdateAvailable = false,
                    latestVersion = BuildConfig.VERSION_NAME,
                    currentVersion = BuildConfig.VERSION_NAME
                )
            }

            val bodyText = response.body?.string().orEmpty()
            val json = JSONObject(bodyText)

            val rawTag = json.optString("tag_name", "").trim()
            val releaseTitle = json.optString("name", rawTag)
            val releaseNotes = json.optString("body", "")

            var apkUrl: String? = null
            var apkFileName: String? = null

            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    val downloadUrl = asset.optString("browser_download_url", "")
                    if (name.endsWith(".apk", ignoreCase = true) || downloadUrl.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = downloadUrl
                        apkFileName = name
                        break
                    }
                }
            }

            val currentVer = BuildConfig.VERSION_NAME
            val isNewer = isNewerVersion(rawTag, currentVer) && !apkUrl.isNullOrBlank()

            val info = UpdateInfo(
                isUpdateAvailable = isNewer,
                latestVersion = rawTag.removePrefix("v").removePrefix("V"),
                currentVersion = currentVer,
                releaseTitle = releaseTitle,
                releaseNotes = releaseNotes,
                apkUrl = apkUrl,
                apkFileName = apkFileName
            )

            _updateState.update {
                it.copy(
                    status = if (isNewer) UpdateDownloadStatus.AVAILABLE else UpdateDownloadStatus.UP_TO_DATE,
                    updateInfo = info,
                    errorMessage = null
                )
            }

            info
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Failed to check GitHub releases"
            _updateState.update { it.copy(status = UpdateDownloadStatus.FAILED, errorMessage = errorMsg) }
            UpdateInfo(
                isUpdateAvailable = false,
                latestVersion = BuildConfig.VERSION_NAME,
                currentVersion = BuildConfig.VERSION_NAME
            )
        }
    }

    /**
     * Downloads the APK using Android's DownloadManager.
     */
    fun startDownload(updateInfo: UpdateInfo) {
        registerDownloadReceiver()
        val apkUrl = updateInfo.apkUrl ?: run {
            _updateState.update { it.copy(status = UpdateDownloadStatus.FAILED, errorMessage = "No APK download URL found") }
            return
        }

        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val targetFileName = updateInfo.apkFileName ?: "RogueTD-v${updateInfo.latestVersion}.apk"
            val cacheTargetDir = context.externalCacheDir ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
            val destFile = File(cacheTargetDir, targetFileName)

            if (destFile.exists()) {
                destFile.delete()
            }

            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("Rogue TD Update v${updateInfo.latestVersion}")
                .setDescription("Downloading latest release APK...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setMimeType("application/vnd.android.package-archive")

            try {
                request.setDestinationUri(Uri.fromFile(destFile))
            } catch (_: Exception) {
                request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, targetFileName)
            }

            activeDownloadId = downloadManager.enqueue(request)

            _updateState.update {
                it.copy(
                    status = UpdateDownloadStatus.DOWNLOADING,
                    progressPercent = 0,
                    downloadedApkFile = destFile,
                    errorMessage = null
                )
            }

            startProgressPolling(downloadManager, activeDownloadId, destFile)
        } catch (e: Exception) {
            _updateState.update {
                it.copy(
                    status = UpdateDownloadStatus.FAILED,
                    errorMessage = "Download initiation failed: ${e.message}"
                )
            }
        }
    }

    private fun startProgressPolling(downloadManager: DownloadManager, downloadId: Long, file: File) {
        progressPollingJob?.cancel()
        progressPollingJob = scope.launch {
            while (isActive) {
                delay(400)
                val query = DownloadManager.Query().setFilterById(downloadId)
                try {
                    val found = downloadManager.query(query)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val bytesCol = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val totalCol = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                            val bytes = if (bytesCol >= 0) cursor.getLong(bytesCol) else 0L
                            val total = if (totalCol >= 0) cursor.getLong(totalCol) else 0L
                            val status = if (statusCol >= 0) cursor.getInt(statusCol) else 0

                            if (total > 0L) {
                                val pct = (bytes * 100L / total).toInt().coerceIn(0, 100)
                                _updateState.update { it.copy(progressPercent = pct) }
                            }

                            when (status) {
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    val uriCol = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                                    val localUriStr = if (uriCol >= 0) cursor.getString(uriCol) else null
                                    val resolvedFile = localUriStr?.let { Uri.parse(it).path?.let { p -> File(p) } } ?: file
                                    val finalFile = if (resolvedFile.exists()) resolvedFile else file
                                    _updateState.update {
                                        it.copy(
                                            status = UpdateDownloadStatus.READY_TO_INSTALL,
                                            progressPercent = 100,
                                            downloadedApkFile = finalFile
                                        )
                                    }
                                    return@launch
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    _updateState.update {
                                        it.copy(
                                            status = UpdateDownloadStatus.FAILED,
                                            errorMessage = "Download failed via DownloadManager"
                                        )
                                    }
                                    return@launch
                                }
                            }
                            true
                        } else {
                            false
                        }
                    } ?: false

                    if (!found) {
                        _updateState.update {
                            it.copy(
                                status = UpdateDownloadStatus.FAILED,
                                errorMessage = "Download was cancelled or removed"
                            )
                        }
                        return@launch
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _updateState.update {
                        it.copy(
                            status = UpdateDownloadStatus.FAILED,
                            errorMessage = "Download polling error: ${e.message}"
                        )
                    }
                    return@launch
                }
            }
        }
    }

    private fun checkDownloadStatus(downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        try {
            downloadManager.query(query)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = if (statusCol >= 0) cursor.getInt(statusCol) else 0
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        val uriCol = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        val localUriStr = if (uriCol >= 0) cursor.getString(uriCol) else null
                        val fallbackFile = _updateState.value.downloadedApkFile
                        val resolvedFile = localUriStr?.let { Uri.parse(it).path?.let { p -> File(p) } } ?: fallbackFile
                        val finalFile = if (resolvedFile != null && resolvedFile.exists()) resolvedFile else fallbackFile
                        if (finalFile != null && finalFile.exists()) {
                            _updateState.update {
                                it.copy(
                                    status = UpdateDownloadStatus.READY_TO_INSTALL,
                                    progressPercent = 100,
                                    downloadedApkFile = finalFile
                                )
                            }
                        }
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        _updateState.update {
                            it.copy(
                                status = UpdateDownloadStatus.FAILED,
                                errorMessage = "Download failed via DownloadManager"
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Builds and launches an Intent(Intent.ACTION_VIEW) using FileProvider to trigger the Android installer.
     */
    fun installApk(file: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            _updateState.update {
                it.copy(
                    status = UpdateDownloadStatus.FAILED,
                    errorMessage = "Package installer launch failed: ${e.message}"
                )
            }
        }
    }

    /**
     * Compares two semantic version tags (e.g. "v0.0.0.005" vs "0.0.0.004").
     */
    fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val remoteParts = parseVersionNumbers(remoteTag)
        val currentParts = parseVersionNumbers(currentVersion)
        val maxLen = maxOf(remoteParts.size, currentParts.size)

        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    private fun parseVersionNumbers(raw: String): List<Int> {
        val stripped = raw.trim().removePrefix("v").removePrefix("V")
        val mainVer = stripped.substringBefore("-").substringBefore("+")
        return mainVer.split(".").mapNotNull { it.toIntOrNull() }
    }
}
