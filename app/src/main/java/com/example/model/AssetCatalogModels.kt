package com.example.model

/**
 * Sections inside the revamped "Archives" tab in the Sound & Skin Vault.
 */
enum class ArchiveSection(val id: String, val label: String) {
    ALL("ALL", "All Archives"),
    TURRET_FIRE("TURRET_FIRE", "Turret Fire"),
    TURRET_UPGRADE("TURRET_UPGRADE", "Upgrades"),
    BGM_SFX("BGM_SFX", "BGM & SFX"),
    TURRET_SKIN("TURRET_SKIN", "Turret Skins"),
    MAP_THEME("MAP_THEME", "Map Themes")
}

/**
 * Data-driven Turret Visual Skin definition rendered by [com.example.ui.canvas.drawTowers]
 * and [com.example.ui.menu.TurretSkinPreviewThumbnail] without per-build renderer branches.
 */
data class TurretSkinDef(
    val id: String,
    val buildCode: Int,
    val buildLabel: String,
    val slotIndex: Int,
    val displayName: String,
    val description: String,
    val drawStyle: TowerDrawStyle,
    val hullColor: Long,
    val frameColor: Long,
    val detailColor: Long,
    val accentTint: Long = 0L,
    val overrideClassTint: Boolean = false,
    val kineticTint: Long = 0L,
    val elementalTint: Long = 0L,
    val siegeTint: Long = 0L,
    val stellarTint: Long = 0L,
    val hasCrosshair: Boolean = false,
    val hasCornerStuds: Boolean = false,
    val hasVoidHalo: Boolean = false,
    val hasPixelCorners: Boolean = false,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false
) {
    fun resolveClassTint(towerClass: TowerClass, fallback: Long): Long {
        if (!overrideClassTint) return fallback
        return when (towerClass) {
            TowerClass.KINETIC_BALLISTIC -> if (kineticTint != 0L) kineticTint else fallback
            TowerClass.ELEMENTAL_VOID -> if (elementalTint != 0L) elementalTint else fallback
            TowerClass.HEAVY_SIEGE -> if (siegeTint != 0L) siegeTint else fallback
            TowerClass.STELLAR_COSMIC -> if (stellarTint != 0L) stellarTint else fallback
        }
    }
}

/**
 * Data-driven Map & Lane Environment Theme definition consumed by [com.example.ui.GameCanvas]
 * and Vault thumbnail badges.
 */
data class MapThemeDef(
    val id: String,
    val buildCode: Int,
    val buildLabel: String,
    val slotIndex: Int,
    val displayName: String,
    val description: String,
    val mapStyle: MapStyle,
    val useBiomePalette: Boolean,
    val groundColor: Long,
    val gridLineColor: Long,
    val gridLineAlpha: Float = 0.7f,
    val pathBaseColor: Long,
    val laneAlphaColor: Long,
    val laneBetaColor: Long,
    val laneMergedColor: Long,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false
)

/**
 * Data-driven Background Music track entry (active + archived builds).
 */
data class BgmTrackDef(
    val id: String,
    val buildCode: Int,
    val buildLabel: String,
    val slotIndex: Int,
    val displayName: String,
    val description: String,
    val wavFileName: String,
    val legacyTrackName: String? = null,
    val isRollingPlaylist: Boolean = false,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false
)

/**
 * Data-driven SFX Pack entry (Default Cyber Synth + Build-over-Build variant packs).
 */
data class SfxPackDef(
    val id: String,
    val buildCode: Int,
    val buildLabel: String,
    val slotIndex: Int,
    val displayName: String,
    val description: String,
    val legacyProfileName: String = "CYBER_SYNTH",
    val previewWavFileName: String = "",
    val isDefault: Boolean = false,
    val isArchived: Boolean = false
)

/**
 * Data-driven Turret Gunfire / Upgrade recipe metadata for active & archived variants.
 */
data class TurretAudioDef(
    val id: String,
    val section: ArchiveSection,
    val towerClass: TowerClass,
    val buildCode: Int,
    val buildLabel: String,
    val slotIndex: Int,
    val displayName: String,
    val description: String,
    val wavFileName: String,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false
)
