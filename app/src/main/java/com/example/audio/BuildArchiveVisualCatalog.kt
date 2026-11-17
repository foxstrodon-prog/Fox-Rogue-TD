package com.example.audio

import com.example.model.MapStyle
import com.example.model.MapThemeDef
import com.example.model.MapThemeSlot
import com.example.model.SkinTheme
import com.example.model.TowerDrawStyle
import com.example.model.TurretSkinDef
import com.example.model.TurretSkinSlot

internal const val PRE60_BUILD_CODE = 0
internal const val PRE60_BUILD_LABEL = "Pre-Build #60"
internal const val B60_BUILD_CODE = 60
internal const val B60_BUILD_LABEL = "Build #60 (v0.0.1.039)"
internal const val CURRENT_BUILD_CODE = 61
internal const val CURRENT_BUILD_LABEL = "Build #61 (v0.0.1.040)"

/**
 * Data-driven Turret Skins & Map Themes across Pre-Build #60, Build #60, and Build #61+.
 * Future builds only append/swap entries here without modifying DrawScope renderers.
 */
internal object BuildArchiveVisualCatalog {

    val defaultTurretSkin = TurretSkinDef(
        id = "default_skin_1",
        buildCode = CURRENT_BUILD_CODE,
        buildLabel = "Default (Core)",
        slotIndex = 1,
        displayName = TurretSkinSlot.SLOT_1_DEFAULT.displayName,
        description = TurretSkinSlot.SLOT_1_DEFAULT.description,
        drawStyle = TowerDrawStyle.NEON_GLOW,
        hullColor = 0xFF0F172A,
        frameColor = 0xFF00F0FF,
        detailColor = 0xFFFFFFFF,
        accentTint = 0xFF00F0FF,
        overrideClassTint = false,
        isDefault = true,
        isArchived = false
    )

    val build61TurretSkins: List<TurretSkinDef> = listOf(
        defaultTurretSkin,
        TurretSkinDef(
            id = "b61_skin_2",
            buildCode = CURRENT_BUILD_CODE,
            buildLabel = CURRENT_BUILD_LABEL,
            slotIndex = 2,
            displayName = "Crimson Vanguard",
            description = "Carbon-crimson composite armor with gilded barrel collars and bolted studs",
            drawStyle = TowerDrawStyle.SOLID_MILITARY,
            hullColor = 0xFF280B12,
            frameColor = 0xFFE11D48,
            detailColor = 0xFFFBBF24,
            accentTint = 0xFFF43F5E,
            overrideClassTint = true,
            kineticTint = 0xFFF43F5E,
            elementalTint = 0xFFFB7185,
            siegeTint = 0xFFF59E0B,
            stellarTint = 0xFFFDE047,
            hasCornerStuds = true
        ),
        TurretSkinDef(
            id = "b61_skin_3",
            buildCode = CURRENT_BUILD_CODE,
            buildLabel = CURRENT_BUILD_LABEL,
            slotIndex = 3,
            displayName = "Arctic Prism",
            description = "Glacial CAD wireframe chassis with frosted cyan crosshairs and ice calibration marks",
            drawStyle = TowerDrawStyle.BLUEPRINT_LINE,
            hullColor = 0xFF082032,
            frameColor = 0xFF22D3EE,
            detailColor = 0xFFE0F2FE,
            accentTint = 0xFF67E8F9,
            overrideClassTint = true,
            kineticTint = 0xFF67E8F9,
            elementalTint = 0xFF38BDF8,
            siegeTint = 0xFFA5F3FC,
            stellarTint = 0xFFBAE6FD,
            hasCrosshair = true
        ),
        TurretSkinDef(
            id = "b61_skin_4",
            buildCode = CURRENT_BUILD_CODE,
            buildLabel = CURRENT_BUILD_LABEL,
            slotIndex = 4,
            displayName = "Astral Eclipse",
            description = "Obsidian corona emitters with orbiting ultraviolet halos and solar-gold cores",
            drawStyle = TowerDrawStyle.VOID_RING,
            hullColor = 0xFF0C051E,
            frameColor = 0xFFC084FC,
            detailColor = 0xFFFDE047,
            accentTint = 0xFFE879F9,
            overrideClassTint = true,
            kineticTint = 0xFFC084FC,
            elementalTint = 0xFFE879F9,
            siegeTint = 0xFFFDE047,
            stellarTint = 0xFFFACC15,
            hasVoidHalo = true
        ),
        TurretSkinDef(
            id = "b61_skin_5",
            buildCode = CURRENT_BUILD_CODE,
            buildLabel = CURRENT_BUILD_LABEL,
            slotIndex = 5,
            displayName = "Phosphor 1984",
            description = "High-contrast CRT phosphor lime & amber pixel blocks with retro cabinet trim",
            drawStyle = TowerDrawStyle.PIXEL_BLOCK,
            hullColor = 0xFF0A140C,
            frameColor = 0xFF4ADE80,
            detailColor = 0xFFFB923C,
            accentTint = 0xFF4ADE80,
            overrideClassTint = true,
            kineticTint = 0xFF4ADE80,
            elementalTint = 0xFF22D3EE,
            siegeTint = 0xFFFB923C,
            stellarTint = 0xFFFACC15,
            hasPixelCorners = true
        )
    )

    val build60ArchivedSkins: List<TurretSkinDef> = listOf(
        defaultTurretSkin.copy(
            id = "b60_skin_1",
            buildCode = B60_BUILD_CODE,
            buildLabel = B60_BUILD_LABEL,
            displayName = "${TurretSkinSlot.SLOT_1_DEFAULT.displayName} [B60 Snapshot]",
            isDefault = false,
            isArchived = true
        ),
        TurretSkinDef(
            id = "b60_skin_2",
            buildCode = B60_BUILD_CODE,
            buildLabel = B60_BUILD_LABEL,
            slotIndex = 2,
            displayName = TurretSkinSlot.SLOT_2_BLUEPRINT.displayName,
            description = TurretSkinSlot.SLOT_2_BLUEPRINT.description,
            drawStyle = TowerDrawStyle.BLUEPRINT_LINE,
            hullColor = 0xFF0A192F,
            frameColor = 0xFF60A5FA,
            detailColor = 0xD9FFFFFF,
            accentTint = 0xFF60A5FA,
            hasCrosshair = true,
            isArchived = true
        ),
        TurretSkinDef(
            id = "b60_skin_3",
            buildCode = B60_BUILD_CODE,
            buildLabel = B60_BUILD_LABEL,
            slotIndex = 3,
            displayName = TurretSkinSlot.SLOT_3_MILITARY.displayName,
            description = TurretSkinSlot.SLOT_3_MILITARY.description,
            drawStyle = TowerDrawStyle.SOLID_MILITARY,
            hullColor = 0xFF1E293B,
            frameColor = 0xFF334155,
            detailColor = 0xFF94A3B8,
            accentTint = 0xFF94A3B8,
            hasCornerStuds = true,
            isArchived = true
        ),
        TurretSkinDef(
            id = "b60_skin_4",
            buildCode = B60_BUILD_CODE,
            buildLabel = B60_BUILD_LABEL,
            slotIndex = 4,
            displayName = TurretSkinSlot.SLOT_4_VOID.displayName,
            description = TurretSkinSlot.SLOT_4_VOID.description,
            drawStyle = TowerDrawStyle.VOID_RING,
            hullColor = 0xFF0D061A,
            frameColor = 0xFFA855F7,
            detailColor = 0xFFD946EF,
            accentTint = 0xFFD946EF,
            hasVoidHalo = true,
            isArchived = true
        ),
        TurretSkinDef(
            id = "b60_skin_5",
            buildCode = B60_BUILD_CODE,
            buildLabel = B60_BUILD_LABEL,
            slotIndex = 5,
            displayName = TurretSkinSlot.SLOT_5_PIXEL.displayName,
            description = TurretSkinSlot.SLOT_5_PIXEL.description,
            drawStyle = TowerDrawStyle.PIXEL_BLOCK,
            hullColor = 0xFF0F172A,
            frameColor = 0xFFFACC15,
            detailColor = 0xFFFFFFFF,
            accentTint = 0xFFFACC15,
            hasPixelCorners = true,
            isArchived = true
        )
    )

    val pre60ArchivedSkins: List<TurretSkinDef> = SkinTheme.entries.mapIndexed { idx, theme ->
        val cfg = theme.config
        TurretSkinDef(
            id = "pre60_skin_${cfg.id}",
            buildCode = PRE60_BUILD_CODE,
            buildLabel = PRE60_BUILD_LABEL,
            slotIndex = idx + 1,
            displayName = "${cfg.displayName} (Legacy Skin)",
            description = cfg.description,
            drawStyle = cfg.kineticStyle,
            hullColor = cfg.hudBackground,
            frameColor = cfg.hudAccent,
            detailColor = cfg.hudText,
            accentTint = cfg.hudAccent,
            overrideClassTint = cfg.overrideTowerColors,
            kineticTint = cfg.kineticColor,
            elementalTint = cfg.elementalColor,
            siegeTint = cfg.siegeColor,
            stellarTint = 0xFFFFD700,
            hasCrosshair = (cfg.kineticStyle == TowerDrawStyle.BLUEPRINT_LINE),
            hasCornerStuds = (cfg.kineticStyle == TowerDrawStyle.SOLID_MILITARY),
            hasVoidHalo = (cfg.kineticStyle == TowerDrawStyle.VOID_RING),
            hasPixelCorners = (cfg.kineticStyle == TowerDrawStyle.PIXEL_BLOCK),
            isArchived = true
        )
    }

    val defaultMapTheme = MapThemeDef(
        id = "default_theme_1",
        buildCode = CURRENT_BUILD_CODE,
        buildLabel = "Default (Core)",
        slotIndex = 1,
        displayName = MapThemeSlot.SLOT_1_DEFAULT.displayName,
        description = MapThemeSlot.SLOT_1_DEFAULT.description,
        mapStyle = MapStyle.GLOW_CONDUIT,
        useBiomePalette = true,
        groundColor = 0xFF090E17,
        gridLineColor = 0xFF1E293B,
        gridLineAlpha = 0.5f,
        pathBaseColor = 0xFF1E293B,
        laneAlphaColor = 0xFF00F0FF,
        laneBetaColor = 0xFFFF6B00,
        laneMergedColor = 0xFFD946EF,
        isDefault = true,
        isArchived = false
    )

    val build61MapThemes: List<MapThemeDef> = listOf(
        defaultMapTheme,
        MapThemeDef(
            id = "b61_theme_2",
            buildCode = CURRENT_BUILD_CODE,
            buildLabel = CURRENT_BUILD_LABEL,
            slotIndex = 2,
            displayName = "Crimson Caldera",
            description = "Obsidian volcanic terrain with ember-coral & cyan tactical channels",
            mapStyle = MapStyle.ROAD,
            useBiomePalette = false,
            groundColor = 0xFF1A0D12,
            gridLineColor = 0xFF3F1D2B,
            gridLineAlpha = 0.7f,
            pathBaseColor = 0xFF31141E,
            laneAlphaColor = 0xFFFB7185,
            laneBetaColor = 0xFF22D3EE,
            laneMergedColor = 0xFFFBBF24
        ),
        MapThemeDef(
            id = "b61_theme_3",
            buildCode = CURRENT_BUILD_CODE,
            buildLabel = CURRENT_BUILD_LABEL,
            slotIndex = 3,
            displayName = "Glacial Schematic",
            description = "Arctic blueprint grid with crisp ice-cyan and solar-amber channels",
            mapStyle = MapStyle.BLUEPRINT,
            useBiomePalette = false,
            groundColor = 0xFF071A26,
            gridLineColor = 0xFF164E63,
            gridLineAlpha = 0.7f,
            pathBaseColor = 0xFF0E2F44,
            laneAlphaColor = 0xFF67E8F9,
            laneBetaColor = 0xFFFDE047,
            laneMergedColor = 0xFF34D399
        ),
        MapThemeDef(
            id = "b61_theme_4",
            buildCode = CURRENT_BUILD_CODE,
            buildLabel = CURRENT_BUILD_LABEL,
            slotIndex = 4,
            displayName = "Astral Horizon",
            description = "Midnight nebula slate with ultraviolet, solar gold, and plasma teal conduits",
            mapStyle = MapStyle.VOID_RIBBON,
            useBiomePalette = false,
            groundColor = 0xFF08041A,
            gridLineColor = 0xFF311068,
            gridLineAlpha = 0.7f,
            pathBaseColor = 0xFF190C3D,
            laneAlphaColor = 0xFFE879F9,
            laneBetaColor = 0xFFFDE047,
            laneMergedColor = 0xFF2DD4BF
        ),
        MapThemeDef(
            id = "b61_theme_5",
            buildCode = CURRENT_BUILD_CODE,
            buildLabel = CURRENT_BUILD_LABEL,
            slotIndex = 5,
            displayName = "Emerald CRT",
            description = "Retro green-phosphor terminal matrix with high-contrast orange & cyan lanes",
            mapStyle = MapStyle.PIXEL_TILES,
            useBiomePalette = false,
            groundColor = 0xFF041008,
            gridLineColor = 0xFF14532D,
            gridLineAlpha = 0.7f,
            pathBaseColor = 0xFF0B2916,
            laneAlphaColor = 0xFF4ADE80,
            laneBetaColor = 0xFFFB923C,
            laneMergedColor = 0xFF38BDF8
        )
    )

    val build60ArchivedThemes: List<MapThemeDef> = listOf(
        defaultMapTheme.copy(
            id = "b60_theme_1",
            buildCode = B60_BUILD_CODE,
            buildLabel = B60_BUILD_LABEL,
            displayName = "${MapThemeSlot.SLOT_1_DEFAULT.displayName} [B60 Snapshot]",
            isDefault = false,
            isArchived = true
        ),
        MapThemeDef(
            id = "b60_theme_2",
            buildCode = B60_BUILD_CODE,
            buildLabel = B60_BUILD_LABEL,
            slotIndex = 2,
            displayName = MapThemeSlot.SLOT_2_BLUEPRINT.displayName,
            description = MapThemeSlot.SLOT_2_BLUEPRINT.description,
            mapStyle = MapStyle.BLUEPRINT,
            useBiomePalette = false,
            groundColor = 0xFF0B192C,
            gridLineColor = 0xFF1E3E62,
            pathBaseColor = 0xFF16253D,
            laneAlphaColor = 0xFF00E5FF,
            laneBetaColor = 0xFFFFD600,
            laneMergedColor = 0xFFFF2A6D,
            isArchived = true
        ),
        MapThemeDef(
            id = "b60_theme_3",
            buildCode = B60_BUILD_CODE,
            buildLabel = B60_BUILD_LABEL,
            slotIndex = 3,
            displayName = MapThemeSlot.SLOT_3_OUTPOST.displayName,
            description = MapThemeSlot.SLOT_3_OUTPOST.description,
            mapStyle = MapStyle.ROAD,
            useBiomePalette = false,
            groundColor = 0xFF1C1917,
            gridLineColor = 0xFF292524,
            pathBaseColor = 0xFF44403C,
            laneAlphaColor = 0xFFF59E0B,
            laneBetaColor = 0xFF06B6D4,
            laneMergedColor = 0xFF84CC16,
            isArchived = true
        ),
        MapThemeDef(
            id = "b60_theme_4",
            buildCode = B60_BUILD_CODE,
            buildLabel = B60_BUILD_LABEL,
            slotIndex = 4,
            displayName = MapThemeSlot.SLOT_4_VOID.displayName,
            description = MapThemeSlot.SLOT_4_VOID.description,
            mapStyle = MapStyle.VOID_RIBBON,
            useBiomePalette = false,
            groundColor = 0xFF090514,
            gridLineColor = 0xFF2E1065,
            pathBaseColor = 0xFF1E103A,
            laneAlphaColor = 0xFFA855F7,
            laneBetaColor = 0xFFF43F5E,
            laneMergedColor = 0xFF06F9D2,
            isArchived = true
        ),
        MapThemeDef(
            id = "b60_theme_5",
            buildCode = B60_BUILD_CODE,
            buildLabel = B60_BUILD_LABEL,
            slotIndex = 5,
            displayName = MapThemeSlot.SLOT_5_MATRIX.displayName,
            description = MapThemeSlot.SLOT_5_MATRIX.description,
            mapStyle = MapStyle.PIXEL_TILES,
            useBiomePalette = false,
            groundColor = 0xFF020B05,
            gridLineColor = 0xFF064E3B,
            pathBaseColor = 0xFF065F46,
            laneAlphaColor = 0xFF10B981,
            laneBetaColor = 0xFF6366F1,
            laneMergedColor = 0xFFFBBF24,
            isArchived = true
        )
    )

    val pre60ArchivedThemes: List<MapThemeDef> = SkinTheme.entries.mapIndexed { idx, theme ->
        val cfg = theme.config
        MapThemeDef(
            id = "pre60_theme_${cfg.id}",
            buildCode = PRE60_BUILD_CODE,
            buildLabel = PRE60_BUILD_LABEL,
            slotIndex = idx + 1,
            displayName = "${cfg.displayName} (Legacy Map)",
            description = cfg.description,
            mapStyle = cfg.mapStyle,
            useBiomePalette = cfg.useBiomePalette,
            groundColor = cfg.ground,
            gridLineColor = cfg.gridLine,
            gridLineAlpha = 0.7f,
            pathBaseColor = cfg.pathBase,
            laneAlphaColor = cfg.laneAlpha,
            laneBetaColor = cfg.laneBeta,
            laneMergedColor = cfg.laneMerged,
            isArchived = true
        )
    }

    val allSkins: List<TurretSkinDef> = build61TurretSkins + build60ArchivedSkins + pre60ArchivedSkins
    val skinsById: Map<String, TurretSkinDef> = allSkins.associateBy { it.id }

    val allThemes: List<MapThemeDef> = build61MapThemes + build60ArchivedThemes + pre60ArchivedThemes
    val themesById: Map<String, MapThemeDef> = allThemes.associateBy { it.id }
}
