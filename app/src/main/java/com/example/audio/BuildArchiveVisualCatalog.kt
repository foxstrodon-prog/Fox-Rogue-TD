package com.example.audio

import com.example.BuildConfig
import com.example.model.MapStyle
import com.example.model.MapThemeDef
import com.example.model.MapThemeSlot
import com.example.model.SkinTheme
import com.example.model.TowerDrawStyle
import com.example.model.TurretSkinDef
import com.example.model.TurretSkinSlot
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val PRE60_BUILD_CODE = 0
internal const val PRE60_BUILD_LABEL = "Pre-Build #60"
internal const val B60_BUILD_CODE = 60
internal const val B60_BUILD_LABEL = "Build #60 (v0.0.1.039)"
internal const val B61_BUILD_CODE = 61
internal const val B61_BUILD_LABEL = "Build #61 (v0.0.1.040)"

internal val CURRENT_BUILD_CODE: Int
    get() = BuildConfig.VERSION_CODE

internal val CURRENT_BUILD_LABEL: String
    get() = "Build #${BuildConfig.VERSION_CODE} (v${BuildConfig.VERSION_NAME})"

internal fun buildLabelFor(buildCode: Int): String = when (buildCode) {
    PRE60_BUILD_CODE -> PRE60_BUILD_LABEL
    B60_BUILD_CODE -> B60_BUILD_LABEL
    B61_BUILD_CODE -> B61_BUILD_LABEL
    62 -> "Build #62 (v0.0.1.041)"
    CURRENT_BUILD_CODE -> CURRENT_BUILD_LABEL
    else -> "Build #$buildCode"
}

/**
 * Data-driven Turret Skins & Map Themes across Pre-Build #60, Build #60, Build #61,
 * and every subsequent buildCode (automatically generating 4 new skins & 4 new map themes
 * per build increment while archiving all prior builds cleanly).
 */
internal object BuildArchiveVisualCatalog {

    private val skinPrefixes = listOf(
        "Solar", "Obsidian", "Cobalt", "Valkyrie", "Nebula", "Aegis", "Cryo", "Ion",
        "Tempest", "Helios", "Apex", "Rift", "Vortex", "Chrono", "Titan", "Zenith",
        "Spectral", "Nova", "Magma", "Pulse", "Phantom", "Quasar", "Prism", "Cyber"
    )
    private val skinSuffixes = listOf(
        "Aegis", "Carapace", "Sentinel", "Bastion", "Vector", "Lancer", "Protocol", "Matrix",
        "Core", "Vanguard", "Forge", "Monolith", "Circuit", "Beacon", "Striker", "Reactor",
        "Plate", "Chassis", "Halo"
    )
    private val themePrefixes = listOf(
        "Obsidian", "Tundra", "Helios", "Nebula", "Tactical", "Cobalt", "Shattered", "Abyssal",
        "Aurora", "Basalt", "Orbital", "Glacier", "Plasma", "Emerald", "Amber", "Stellar",
        "Vector", "Ionized", "Quantum", "Crimson", "Solaris", "Cinder", "Chrono"
    )
    private val themeSuffixes = listOf(
        "Grid", "Caldera", "Expanse", "Schematic", "Horizon", "Matrix", "Corridor", "Sector",
        "Trench", "Citadel", "Nexus", "Bastion", "Conduit", "Terrace", "Relay", "Front",
        "Vault"
    )

    private val skinIdRegex = Regex("^b(\\d+)_skin_(\\d+)$")
    private val themeIdRegex = Regex("^b(\\d+)_theme_(\\d+)$")

    val defaultTurretSkin: TurretSkinDef
        get() = TurretSkinDef(
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

    private val build61CuratedVariantSkins: List<TurretSkinDef> = listOf(
        TurretSkinDef(
            id = "b61_skin_2",
            buildCode = B61_BUILD_CODE,
            buildLabel = B61_BUILD_LABEL,
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
            buildCode = B61_BUILD_CODE,
            buildLabel = B61_BUILD_LABEL,
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
            buildCode = B61_BUILD_CODE,
            buildLabel = B61_BUILD_LABEL,
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
            buildCode = B61_BUILD_CODE,
            buildLabel = B61_BUILD_LABEL,
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

    fun variantSkinsForBuild(buildCode: Int, isArchived: Boolean): List<TurretSkinDef> {
        if (buildCode <= B60_BUILD_CODE) return emptyList()
        if (buildCode == B61_BUILD_CODE) {
            return build61CuratedVariantSkins.map { it.copy(isArchived = isArchived) }
        }
        val label = buildLabelFor(buildCode)
        val styles = listOf(
            TowerDrawStyle.SOLID_MILITARY,
            TowerDrawStyle.BLUEPRINT_LINE,
            TowerDrawStyle.VOID_RING,
            TowerDrawStyle.PIXEL_BLOCK,
            TowerDrawStyle.NEON_GLOW
        )
        return (2..5).map { slot ->
            val h = deterministicHash(buildCode, slot, 0x51A7)
            val baseHue = ((buildCode * 47f) + ((slot - 2) * 89f) + (h % 29)) % 360f
            val style = styles[(buildCode + slot - 2) % styles.size]
            val pIdx = abs((buildCode * 5 + (slot - 2) * 7) % skinPrefixes.size)
            val sIdx = abs((buildCode * 3 + (slot - 2) * 5 + 4) % skinSuffixes.size)
            val name = "${skinPrefixes[pIdx]} ${skinSuffixes[sIdx]}"
            val hull = hsvToArgb(baseHue, 0.58f, 0.12f)
            val frame = hsvToArgb(baseHue, 0.86f, 0.94f)
            val detailHue = (baseHue + 145f) % 360f
            val detail = hsvToArgb(detailHue, 0.72f, 0.98f)
            val accent = hsvToArgb((baseHue + 22f) % 360f, 0.90f, 0.96f)
            TurretSkinDef(
                id = "b${buildCode}_skin_$slot",
                buildCode = buildCode,
                buildLabel = label,
                slotIndex = slot,
                displayName = name,
                description = "Build #$buildCode procedural ${style.name.lowercase().replace('_', ' ')} armor plating & class accents",
                drawStyle = style,
                hullColor = hull,
                frameColor = frame,
                detailColor = detail,
                accentTint = accent,
                overrideClassTint = true,
                kineticTint = frame,
                elementalTint = hsvToArgb((baseHue + 55f) % 360f, 0.84f, 0.96f),
                siegeTint = detail,
                stellarTint = hsvToArgb((baseHue + 210f) % 360f, 0.80f, 0.99f),
                hasCrosshair = (style == TowerDrawStyle.BLUEPRINT_LINE),
                hasCornerStuds = (style == TowerDrawStyle.SOLID_MILITARY),
                hasVoidHalo = (style == TowerDrawStyle.VOID_RING),
                hasPixelCorners = (style == TowerDrawStyle.PIXEL_BLOCK),
                isDefault = false,
                isArchived = isArchived
            )
        }
    }

    fun activeTurretSkinsFor(targetBuildCode: Int = CURRENT_BUILD_CODE): List<TurretSkinDef> =
        listOf(defaultTurretSkin.copy(buildCode = targetBuildCode)) +
            variantSkinsForBuild(targetBuildCode, isArchived = false)

    fun archivedTurretSkinsFor(targetBuildCode: Int = CURRENT_BUILD_CODE): List<TurretSkinDef> =
        buildList {
            if (targetBuildCode > B60_BUILD_CODE) {
                addAll(build60ArchivedSkins)
            }
            for (b in B61_BUILD_CODE until targetBuildCode) {
                addAll(variantSkinsForBuild(b, isArchived = true))
            }
            addAll(pre60ArchivedSkins)
        }

    val activeTurretSkins: List<TurretSkinDef>
        get() = activeTurretSkinsFor(CURRENT_BUILD_CODE)

    val archivedTurretSkins: List<TurretSkinDef>
        get() = archivedTurretSkinsFor(CURRENT_BUILD_CODE)

    @Deprecated("Use activeTurretSkins", ReplaceWith("activeTurretSkins"))
    val build61TurretSkins: List<TurretSkinDef>
        get() = activeTurretSkins

    val build60ArchivedSkins: List<TurretSkinDef> = listOf(
        TurretSkinDef(
            id = "b60_skin_1",
            buildCode = B60_BUILD_CODE,
            buildLabel = B60_BUILD_LABEL,
            slotIndex = 1,
            displayName = "${TurretSkinSlot.SLOT_1_DEFAULT.displayName} [B60 Snapshot]",
            description = TurretSkinSlot.SLOT_1_DEFAULT.description,
            drawStyle = TowerDrawStyle.NEON_GLOW,
            hullColor = 0xFF0F172A,
            frameColor = 0xFF00F0FF,
            detailColor = 0xFFFFFFFF,
            accentTint = 0xFF00F0FF,
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

    val defaultMapTheme: MapThemeDef
        get() = MapThemeDef(
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

    private val build61CuratedVariantThemes: List<MapThemeDef> = listOf(
        MapThemeDef(
            id = "b61_theme_2",
            buildCode = B61_BUILD_CODE,
            buildLabel = B61_BUILD_LABEL,
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
            buildCode = B61_BUILD_CODE,
            buildLabel = B61_BUILD_LABEL,
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
            buildCode = B61_BUILD_CODE,
            buildLabel = B61_BUILD_LABEL,
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
            buildCode = B61_BUILD_CODE,
            buildLabel = B61_BUILD_LABEL,
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

    fun variantThemesForBuild(buildCode: Int, isArchived: Boolean): List<MapThemeDef> {
        if (buildCode <= B60_BUILD_CODE) return emptyList()
        if (buildCode == B61_BUILD_CODE) {
            return build61CuratedVariantThemes.map { it.copy(isArchived = isArchived) }
        }
        val label = buildLabelFor(buildCode)
        val styles = listOf(
            MapStyle.ROAD,
            MapStyle.BLUEPRINT,
            MapStyle.VOID_RIBBON,
            MapStyle.PIXEL_TILES,
            MapStyle.GLOW_CONDUIT
        )
        return (2..5).map { slot ->
            val h = deterministicHash(buildCode, slot, 0x7E4E)
            val baseHue = ((buildCode * 53f) + ((slot - 2) * 91f) + (h % 31)) % 360f
            val style = styles[(buildCode + slot - 1) % styles.size]
            val pIdx = abs((buildCode * 7 + (slot - 2) * 5) % themePrefixes.size)
            val sIdx = abs((buildCode * 3 + (slot - 2) * 7 + 3) % themeSuffixes.size)
            val name = "${themePrefixes[pIdx]} ${themeSuffixes[sIdx]}"
            val ground = hsvToArgb(baseHue, 0.56f, 0.09f)
            val grid = hsvToArgb(baseHue, 0.62f, 0.25f)
            val pathBase = hsvToArgb(baseHue, 0.54f, 0.18f)
            val laneAlpha = hsvToArgb((baseHue + 35f) % 360f, 0.82f, 0.96f)
            val laneBeta = hsvToArgb((baseHue + 165f) % 360f, 0.85f, 0.97f)
            val laneMerged = hsvToArgb((baseHue + 280f) % 360f, 0.82f, 0.95f)
            MapThemeDef(
                id = "b${buildCode}_theme_$slot",
                buildCode = buildCode,
                buildLabel = label,
                slotIndex = slot,
                displayName = name,
                description = "Build #$buildCode procedural ${style.name.lowercase().replace('_', ' ')} dual-lane arena palette",
                mapStyle = style,
                useBiomePalette = false,
                groundColor = ground,
                gridLineColor = grid,
                gridLineAlpha = 0.7f,
                pathBaseColor = pathBase,
                laneAlphaColor = laneAlpha,
                laneBetaColor = laneBeta,
                laneMergedColor = laneMerged,
                isDefault = false,
                isArchived = isArchived
            )
        }
    }

    fun activeMapThemesFor(targetBuildCode: Int = CURRENT_BUILD_CODE): List<MapThemeDef> =
        listOf(defaultMapTheme.copy(buildCode = targetBuildCode)) +
            variantThemesForBuild(targetBuildCode, isArchived = false)

    fun archivedMapThemesFor(targetBuildCode: Int = CURRENT_BUILD_CODE): List<MapThemeDef> =
        buildList {
            if (targetBuildCode > B60_BUILD_CODE) {
                addAll(build60ArchivedThemes)
            }
            for (b in B61_BUILD_CODE until targetBuildCode) {
                addAll(variantThemesForBuild(b, isArchived = true))
            }
            addAll(pre60ArchivedThemes)
        }

    val activeMapThemes: List<MapThemeDef>
        get() = activeMapThemesFor(CURRENT_BUILD_CODE)

    val archivedMapThemes: List<MapThemeDef>
        get() = archivedMapThemesFor(CURRENT_BUILD_CODE)

    @Deprecated("Use activeMapThemes", ReplaceWith("activeMapThemes"))
    val build61MapThemes: List<MapThemeDef>
        get() = activeMapThemes

    val build60ArchivedThemes: List<MapThemeDef> = listOf(
        MapThemeDef(
            id = "b60_theme_1",
            buildCode = B60_BUILD_CODE,
            buildLabel = B60_BUILD_LABEL,
            slotIndex = 1,
            displayName = "${MapThemeSlot.SLOT_1_DEFAULT.displayName} [B60 Snapshot]",
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

    val allSkins: List<TurretSkinDef>
        get() = activeTurretSkins + archivedTurretSkins

    val skinsById: Map<String, TurretSkinDef>
        get() = allSkins.associateBy { it.id }

    val allThemes: List<MapThemeDef>
        get() = activeMapThemes + archivedMapThemes

    val themesById: Map<String, MapThemeDef>
        get() = allThemes.associateBy { it.id }

    fun findOrGenerateSkinById(id: String): TurretSkinDef? {
        skinsById[id]?.let { return it }
        val match = skinIdRegex.matchEntire(id) ?: return null
        val build = match.groupValues[1].toIntOrNull() ?: return null
        val slot = match.groupValues[2].toIntOrNull() ?: return null
        if (build >= B61_BUILD_CODE && slot in 2..5) {
            return variantSkinsForBuild(build, isArchived = build < CURRENT_BUILD_CODE)
                .firstOrNull { it.slotIndex == slot }
        }
        return null
    }

    fun findOrGenerateThemeById(id: String): MapThemeDef? {
        themesById[id]?.let { return it }
        val match = themeIdRegex.matchEntire(id) ?: return null
        val build = match.groupValues[1].toIntOrNull() ?: return null
        val slot = match.groupValues[2].toIntOrNull() ?: return null
        if (build >= B61_BUILD_CODE && slot in 2..5) {
            return variantThemesForBuild(build, isArchived = build < CURRENT_BUILD_CODE)
                .firstOrNull { it.slotIndex == slot }
        }
        return null
    }

    private fun deterministicHash(buildCode: Int, slot: Int, salt: Int): Int {
        var x = buildCode * 73856093 xor slot * 19349663 xor salt * 83492791
        x = (x xor (x ushr 16)) * 0x45d9f3b
        x = (x xor (x ushr 16)) * 0x45d9f3b
        return abs(x xor (x ushr 16))
    }

    private fun hsvToArgb(hueDeg: Float, saturation: Float, value: Float): Long {
        val h = ((hueDeg % 360f) + 360f) % 360f
        val s = saturation.coerceIn(0f, 1f)
        val v = value.coerceIn(0f, 1f)
        val c = v * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = v - c
        val (rf, gf, bf) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val r = ((rf + m) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((gf + m) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((bf + m) * 255f).roundToInt().coerceIn(0, 255)
        return (0xFFL shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
    }
}
