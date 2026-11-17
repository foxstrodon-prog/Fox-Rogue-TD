package com.example.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.EnumMap
import kotlin.math.abs

/**
 * Encapsulates streaming BGM [MediaPlayer] (rolling playlist, smooth fades, ducking, pause attenuation)
 * and the single-slot transient archive preview [MediaPlayer].
 */
internal class MusicAndPreviewPlayer(
    private val archiveStore: SoundArchiveStore,
    private val isEnabled: () -> Boolean,
    private val isMuted: () -> Boolean,
    private val isSuspended: () -> Boolean,
    private val isPaused: () -> Boolean,
    private val getMasterVolume: () -> Float,
    private val getSfxVolume: () -> Float,
    private val getMusicVolume: () -> Float,
    private val getActiveMusicTrack: () -> MusicTrack,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val playerLock = Any()
    private val playerScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val musicFileLocks = EnumMap<MusicTrack, Any>(MusicTrack::class.java).apply {
        MusicTrack.entries.forEach { put(it, Any()) }
    }
    private var mediaPlayer: MediaPlayer? = null
    private var previewPlayer: MediaPlayer? = null
    private var bgmPausedForPreview: Boolean = false
    private var previewGeneration: Long = 0L
    private var trackGeneration: Long = 0L
    private var isReleased: Boolean = false

    private val rollingPlaylistTracks = listOf(
        MusicTrack.CYBER_AMBIENT,
        MusicTrack.PULSE_ARPEGGIO,
        MusicTrack.DEEP_SPACE,
        MusicTrack.NEON_OVERDRIVE,
        MusicTrack.CELESTIAL_BASTION
    )
    private var currentRollingIndex: Int = 0
    private var playlistTrackElapsedMs: Long = 0L
    private val PLAYLIST_TRACK_WINDOW_MS = 60_000L

    private var currentMusicVolume: Float = 0f
    private var targetMusicVolume: Float = 0f

    @Volatile
    var isDucked: Boolean = false

    private var duckUntilMs: Long = 0L
    private val DUCK_FACTOR = 0.50f
    private val PAUSE_ATTENUATION_FACTOR = 0.50f

    fun getCurrentRollingTrack(): MusicTrack = synchronized(playerLock) {
        rollingPlaylistTracks[currentRollingIndex]
    }

    fun hasActiveMediaPlayer(): Boolean = synchronized(playerLock) {
        mediaPlayer != null
    }

    internal fun hasActivePreviewPlayer(): Boolean = synchronized(playerLock) {
        previewPlayer != null
    }

    internal fun getPreviewPlayerForTest(): MediaPlayer? = synchronized(playerLock) {
        previewPlayer
    }

    fun resetPlaylistElapsed() {
        synchronized(playerLock) {
            playlistTrackElapsedMs = 0L
        }
    }

    fun nextPreviewGeneration(): Long = synchronized(playerLock) {
        ++previewGeneration
    }

    fun nextTrackGeneration(): Long = synchronized(playerLock) {
        ++trackGeneration
    }

    fun getOrCreateMusicFile(track: MusicTrack): File {
        val file = archiveStore.getMusicFile(track)
        if (file.exists() && file.length() >= 256L) {
            return file
        }
        val fileLock = musicFileLocks[track] ?: playerLock
        synchronized(fileLock) {
            if (file.exists() && file.length() >= 256L) {
                return file
            }
            file.parentFile?.let { if (!it.exists()) it.mkdirs() }
            val tmpFile = File(file.parentFile, "${file.name}.${System.nanoTime()}.tmp")
            try {
                val wavBytes = AudioSynthesizer.toWav(AudioSynthesizer.synthesizeMusic(track))
                FileOutputStream(tmpFile).use { it.write(wavBytes) }
                if (!tmpFile.renameTo(file)) {
                    if (file.exists()) {
                        file.delete()
                    }
                    if (!tmpFile.renameTo(file)) {
                        FileOutputStream(file).use { it.write(wavBytes) }
                    }
                }
            } finally {
                if (tmpFile.exists()) {
                    tmpFile.delete()
                }
            }
            return file
        }
    }

    fun playTrack(track: MusicTrack, expectedGeneration: Long? = null) {
        val (target, expectedGen) = synchronized(playerLock) {
            if (expectedGeneration != null && expectedGeneration != trackGeneration) {
                return
            }
            val gen = expectedGeneration ?: ++trackGeneration
            if (isReleased || track == MusicTrack.NONE) {
                stopMusicLocked()
                return
            }
            val resolved = if (track == MusicTrack.ROLLING_PLAYLIST) rollingPlaylistTracks[currentRollingIndex] else track
            if (!isEnabled() || isMuted() || isSuspended()) {
                stopMusicLocked()
                return
            }
            resolved to gen
        }
        playTrackInternal(target, expectedGen)
    }

    private fun playTrackInternal(targetTrack: MusicTrack, expectedTrackGen: Long? = null) {
        val canProceed = synchronized(playerLock) {
            if (expectedTrackGen != null && expectedTrackGen != trackGeneration) {
                false
            } else {
                stopMusicLocked()
                !isReleased && isEnabled() && !isMuted() && !isSuspended() && targetTrack != MusicTrack.NONE
            }
        }
        if (!canProceed) return

        val file = try {
            getOrCreateMusicFile(targetTrack)
        } catch (_: Exception) {
            return
        }

        synchronized(playerLock) {
            if (isReleased || !isEnabled() || isMuted() || isSuspended() || targetTrack == MusicTrack.NONE) return
            if (expectedTrackGen != null && expectedTrackGen != trackGeneration) return

            var candidate: MediaPlayer? = null
            try {
                val mp = MediaPlayer()
                candidate = mp
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                mp.setDataSource(file.absolutePath)
                mp.isLooping = true
                mp.prepare()
                targetMusicVolume = calculateEffectiveMusicVolume()
                currentMusicVolume = 0f
                mp.setVolume(0f, 0f)
                if (!isSuspended() && !bgmPausedForPreview) {
                    mp.start()
                }
                mediaPlayer = mp
                candidate = null
            } catch (_: Exception) {
                try { candidate?.release() } catch (_: Exception) {}
                mediaPlayer = null
            }
        }
    }

    fun playTransientWavFile(file: File, expectedGeneration: Long? = null) {
        synchronized(playerLock) {
            if (isReleased || !file.exists() || !isEnabled() || isMuted() || isSuspended()) return
            if (expectedGeneration != null && expectedGeneration != previewGeneration) return
            if (expectedGeneration == null) {
                previewGeneration++
            }

            stopPreviewPlayerOnlyLocked()

            val wasBgmPlaying = try { mediaPlayer?.isPlaying == true } catch (_: Exception) { false }
            if (wasBgmPlaying) {
                try { mediaPlayer?.pause() } catch (_: Exception) {}
                bgmPausedForPreview = true
            }

            var candidate: MediaPlayer? = null
            try {
                val mp = MediaPlayer()
                candidate = mp
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                mp.setDataSource(file.absolutePath)
                mp.prepare()
                val vol = (getMasterVolume() * getSfxVolume()).coerceIn(0f, 1f)
                mp.setVolume(vol, vol)
                mp.setOnCompletionListener { completedMp ->
                    synchronized(playerLock) {
                        try { completedMp.release() } catch (_: Exception) {}
                        if (previewPlayer === completedMp) previewPlayer = null
                        resumeBgmAfterPreviewLocked()
                    }
                }
                mp.setOnErrorListener { failedMp, _, _ ->
                    synchronized(playerLock) {
                        try { failedMp.release() } catch (_: Exception) {}
                        if (previewPlayer === failedMp) previewPlayer = null
                        resumeBgmAfterPreviewLocked()
                    }
                    true
                }
                mp.start()
                previewPlayer = mp
                candidate = null
            } catch (_: Exception) {
                try { candidate?.release() } catch (_: Exception) {}
                previewPlayer = null
                resumeBgmAfterPreviewLocked()
            }
        }
    }

    fun stopPreview() {
        synchronized(playerLock) {
            previewGeneration++
            stopPreviewPlayerOnlyLocked()
            resumeBgmAfterPreviewLocked()
        }
    }

    private fun stopPreviewPlayerOnlyLocked() {
        previewPlayer?.let { mp ->
            try { if (mp.isPlaying) mp.stop() } catch (_: Exception) {}
            try { mp.release() } catch (_: Exception) {}
        }
        previewPlayer = null
    }

    private fun resumeBgmAfterPreviewLocked() {
        if (bgmPausedForPreview) {
            bgmPausedForPreview = false
            ensureBgmPlayingIfEligibleLocked()
        }
    }

    fun ensureBgmPlayingIfEligible() {
        synchronized(playerLock) {
            ensureBgmPlayingIfEligibleLocked()
        }
    }

    private fun ensureBgmPlayingIfEligibleLocked() {
        if (!isReleased && !isSuspended() && !bgmPausedForPreview && isEnabled() && !isMuted() && getActiveMusicTrack() != MusicTrack.NONE) {
            try {
                if (mediaPlayer?.isPlaying == false) {
                    mediaPlayer?.start()
                }
            } catch (_: Exception) {}
        }
    }

    fun suspendMusic() {
        synchronized(playerLock) {
            trackGeneration++
            previewGeneration++
            stopPreviewPlayerOnlyLocked()
            bgmPausedForPreview = false
            try { mediaPlayer?.pause() } catch (_: Exception) {}
        }
    }

    fun resumeMusic() {
        val trackToPlay: Pair<MusicTrack, Long>? = synchronized(playerLock) {
            if (isReleased || !isEnabled() || isMuted() || isSuspended()) return
            val track = getActiveMusicTrack()
            if (track == MusicTrack.NONE) return
            if (mediaPlayer == null) {
                val resolved = if (track == MusicTrack.ROLLING_PLAYLIST) rollingPlaylistTracks[currentRollingIndex] else track
                resolved to ++trackGeneration
            } else {
                try {
                    if (mediaPlayer?.isPlaying == false && !bgmPausedForPreview) {
                        val vol = calculateEffectiveMusicVolume()
                        currentMusicVolume = vol
                        targetMusicVolume = vol
                        mediaPlayer?.setVolume(vol, vol)
                        mediaPlayer?.start()
                    }
                } catch (_: Exception) {}
                null
            }
        }
        trackToPlay?.let { (track, gen) ->
            playerScope.launch {
                playTrackInternal(track, gen)
            }
        }
    }

    fun triggerCombatDucking(durationMs: Long = 450L) {
        synchronized(playerLock) {
            duckUntilMs = System.currentTimeMillis() + durationMs
            if (!isDucked) {
                isDucked = true
                updateMusicVolumeTargetLocked()
            }
        }
    }

    fun updateTick(nowMs: Long, dtMs: Long) {
        var nextRollingTrack: Pair<MusicTrack, Long>? = null
        synchronized(playerLock) {
            if (isReleased) return
            if (isDucked && nowMs >= duckUntilMs) {
                isDucked = false
                updateMusicVolumeTargetLocked()
            }

            val fadeStep = (dtMs / 800f).coerceIn(0.015f, 1.0f)
            if (abs(targetMusicVolume - currentMusicVolume) > 0.005f) {
                currentMusicVolume += (targetMusicVolume - currentMusicVolume) * fadeStep
                try {
                    mediaPlayer?.setVolume(currentMusicVolume, currentMusicVolume)
                } catch (_: Exception) {}
            } else {
                currentMusicVolume = targetMusicVolume
            }

            if (getActiveMusicTrack() == MusicTrack.ROLLING_PLAYLIST &&
                isEnabled() && !isMuted() && !isPaused() && !isSuspended() && !bgmPausedForPreview
            ) {
                playlistTrackElapsedMs += dtMs
                if (playlistTrackElapsedMs >= PLAYLIST_TRACK_WINDOW_MS) {
                    currentRollingIndex = (currentRollingIndex + 1) % rollingPlaylistTracks.size
                    playlistTrackElapsedMs = 0L
                    nextRollingTrack = rollingPlaylistTracks[currentRollingIndex] to ++trackGeneration
                }
            }
        }
        nextRollingTrack?.let { (track, gen) ->
            playerScope.launch {
                playTrackInternal(track, gen)
            }
        }
    }

    fun stopMusic() {
        synchronized(playerLock) {
            trackGeneration++
            stopMusicLocked()
        }
    }

    private fun stopMusicLocked() {
        mediaPlayer?.let { mp ->
            try { if (mp.isPlaying) mp.stop() } catch (_: Exception) {}
            try { mp.release() } catch (_: Exception) {}
        }
        mediaPlayer = null
    }

    fun calculateEffectiveMusicVolume(): Float {
        if (!isEnabled() || isMuted() || isSuspended() || getActiveMusicTrack() == MusicTrack.NONE) return 0f
        val base = (getMasterVolume() * getMusicVolume()).coerceIn(0f, 1f)
        val pauseFactor = if (isPaused()) PAUSE_ATTENUATION_FACTOR else 1.0f
        val duckFactor = if (isDucked && !isPaused()) DUCK_FACTOR else 1.0f
        return (base * pauseFactor * duckFactor).coerceIn(0f, 1f)
    }

    fun updateMusicVolumeTarget() {
        synchronized(playerLock) {
            updateMusicVolumeTargetLocked()
        }
    }

    private fun updateMusicVolumeTargetLocked() {
        targetMusicVolume = calculateEffectiveMusicVolume()
    }

    fun updateMusicVolume() {
        synchronized(playerLock) {
            updateMusicVolumeTargetLocked()
            currentMusicVolume = targetMusicVolume
            try {
                mediaPlayer?.setVolume(currentMusicVolume, currentMusicVolume)
            } catch (_: Exception) {}
        }
    }

    fun release() {
        playerScope.cancel()
        synchronized(playerLock) {
            isReleased = true
            trackGeneration++
            previewGeneration++
            stopPreviewPlayerOnlyLocked()
            bgmPausedForPreview = false
            stopMusicLocked()
        }
    }
}
