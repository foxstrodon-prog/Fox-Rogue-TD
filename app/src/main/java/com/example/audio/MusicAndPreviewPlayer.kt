package com.example.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import java.io.File
import java.io.FileOutputStream

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
    private val getActiveMusicTrack: () -> MusicTrack
) {
    private var mediaPlayer: MediaPlayer? = null
    private var previewPlayer: MediaPlayer? = null
    private var bgmPausedForPreview: Boolean = false

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

    fun getCurrentRollingTrack(): MusicTrack = rollingPlaylistTracks[currentRollingIndex]

    fun hasActiveMediaPlayer(): Boolean = mediaPlayer != null

    fun resetPlaylistElapsed() {
        playlistTrackElapsedMs = 0L
    }

    fun getOrCreateMusicFile(track: MusicTrack): File {
        val file = archiveStore.getMusicFile(track)
        if (!file.exists() || file.length() < 256L) {
            FileOutputStream(file).use {
                it.write(AudioSynthesizer.toWav(AudioSynthesizer.synthesizeMusic(track)))
            }
        }
        return file
    }

    fun playTrack(track: MusicTrack) {
        if (track == MusicTrack.NONE) {
            stopMusic()
            return
        }
        val target = if (track == MusicTrack.ROLLING_PLAYLIST) rollingPlaylistTracks[currentRollingIndex] else track
        playTrackInternal(target)
    }

    private fun playTrackInternal(targetTrack: MusicTrack) {
        try {
            mediaPlayer?.let { mp ->
                try { if (mp.isPlaying) mp.stop() } catch (_: Exception) {}
                mp.release()
            }
            mediaPlayer = null

            if (!isEnabled() || isMuted() || isSuspended() || targetTrack == MusicTrack.NONE) return

            val file = getOrCreateMusicFile(targetTrack)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(file.absolutePath)
                isLooping = true
                prepare()
                targetMusicVolume = calculateEffectiveMusicVolume()
                currentMusicVolume = 0f
                setVolume(0f, 0f)
                if (!isSuspended() && !bgmPausedForPreview) {
                    start()
                }
            }
        } catch (_: Exception) {
            mediaPlayer = null
        }
    }

    fun playTransientWavFile(file: File) {
        if (!file.exists() || !isEnabled() || isMuted()) return
        try {
            previewPlayer?.let { mp ->
                try { if (mp.isPlaying) mp.stop() } catch (_: Exception) {}
                mp.release()
            }
            previewPlayer = null

            if (mediaPlayer?.isPlaying == true) {
                try { mediaPlayer?.pause() } catch (_: Exception) {}
                bgmPausedForPreview = true
            }

            previewPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
                val vol = (getMasterVolume() * getSfxVolume()).coerceIn(0.15f, 1f)
                setVolume(vol, vol)
                start()
                setOnCompletionListener { mp ->
                    mp.release()
                    if (previewPlayer === mp) previewPlayer = null
                    resumeBgmAfterPreview()
                }
            }
        } catch (_: Exception) {
            previewPlayer = null
            resumeBgmAfterPreview()
        }
    }

    fun stopPreview() {
        try {
            previewPlayer?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        } catch (_: Exception) {}
        previewPlayer = null
        resumeBgmAfterPreview()
    }

    private fun resumeBgmAfterPreview() {
        if (bgmPausedForPreview) {
            bgmPausedForPreview = false
            ensureBgmPlayingIfEligible()
        }
    }

    fun ensureBgmPlayingIfEligible() {
        if (!isSuspended() && !bgmPausedForPreview && isEnabled() && !isMuted() && getActiveMusicTrack() != MusicTrack.NONE) {
            try {
                if (mediaPlayer?.isPlaying == false) {
                    mediaPlayer?.start()
                }
            } catch (_: Exception) {}
        }
    }

    fun suspendMusic() {
        stopPreview()
        try { mediaPlayer?.pause() } catch (_: Exception) {}
    }

    fun resumeMusic() {
        if (!isEnabled() || isMuted()) return
        val track = getActiveMusicTrack()
        if (track != MusicTrack.NONE) {
            try {
                if (mediaPlayer == null) {
                    playTrack(track)
                } else if (mediaPlayer?.isPlaying == false && !bgmPausedForPreview) {
                    val vol = calculateEffectiveMusicVolume()
                    currentMusicVolume = vol
                    targetMusicVolume = vol
                    mediaPlayer?.setVolume(vol, vol)
                    mediaPlayer?.start()
                }
            } catch (_: Exception) {}
        }
    }

    fun triggerCombatDucking(durationMs: Long = 450L) {
        duckUntilMs = System.currentTimeMillis() + durationMs
        if (!isDucked) {
            isDucked = true
            updateMusicVolumeTarget()
        }
    }

    fun updateTick(nowMs: Long, dtMs: Long) {
        if (isDucked && nowMs >= duckUntilMs) {
            isDucked = false
            updateMusicVolumeTarget()
        }

        val fadeStep = (dtMs / 800f).coerceIn(0.015f, 1.0f)
        if (kotlin.math.abs(targetMusicVolume - currentMusicVolume) > 0.005f) {
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
                playTrackInternal(rollingPlaylistTracks[currentRollingIndex])
            }
        }
    }

    fun stopMusic() {
        try {
            mediaPlayer?.let { mp ->
                try { if (mp.isPlaying) mp.stop() } catch (_: Exception) {}
                mp.release()
            }
        } catch (_: Exception) {}
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
        targetMusicVolume = calculateEffectiveMusicVolume()
    }

    fun updateMusicVolume() {
        updateMusicVolumeTarget()
        currentMusicVolume = targetMusicVolume
        try {
            mediaPlayer?.setVolume(currentMusicVolume, currentMusicVolume)
        } catch (_: Exception) {}
    }

    fun release() {
        stopPreview()
        stopMusic()
    }
}
