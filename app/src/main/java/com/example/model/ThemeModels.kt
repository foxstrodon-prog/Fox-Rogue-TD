package com.example.model

enum class TowerDrawStyle { NEON_GLOW, BLUEPRINT_LINE, SOLID_MILITARY, VOID_RING, PIXEL_BLOCK }
enum class ProjectileStyle { ORB, TRACER, SHELL, DIAMOND, PIXEL }
enum class HudStyle { GLASS, OUTLINE, PANEL, VOID_GLOW, PIXEL }
enum class MapStyle { GLOW_CONDUIT, BLUEPRINT, ROAD, VOID_RIBBON, PIXEL_TILES }

/**
 * Full procedural palette + draw-style definition for one theme.
 * Covers (a) UI HUD, (b) map grid/paths, (c) Tower 1 Kinetic, (d) Tower 2 Elemental, (e) Tower 3 Siege.
 * All colours are packed ARGB longs (use Color(value)).
 */
data class ThemeSkinConfig(
    val id: String,
    val displayName: String,
    val description: String,
    // (b) Map grid / paths
    /** When true the per-run Biome palette is used for ground/path colours (Cyberpunk Neon only). */
    val useBiomePalette: Boolean,
    val mapStyle: MapStyle,
    val ground: Long,
    val groundAlt: Long,
    val gridLine: Long,
    val pathBase: Long,
    val laneAlpha: Long,
    val laneBeta: Long,
    val laneMerged: Long,
    val obstacle: Long,
    val clearanceMark: Long,
    // (a) HUD
    val hudStyle: HudStyle,
    val hudCornerDp: Int,
    val hudBackground: Long,
    val hudSurface: Long,
    val hudBorder: Long,
    val hudAccent: Long,
    val hudText: Long,
    val hudTextDim: Long,
    // (c)(d)(e) Towers
    /** When false, towers keep their per-type/archetype colours (Cyberpunk Neon). */
    val overrideTowerColors: Boolean,
    val kineticColor: Long,
    val elementalColor: Long,
    val siegeColor: Long,
    val kineticStyle: TowerDrawStyle,
    val elementalStyle: TowerDrawStyle,
    val siegeStyle: TowerDrawStyle,
    // Juice
    val projectileStyle: ProjectileStyle,
    val shockwaveColor: Long,
    /** Square (pixel) shockwave instead of round ring. */
    val shockwaveSquare: Boolean = false
) {
    fun towerColor(cls: TowerClass): Long = when (cls) {
        TowerClass.KINETIC_BALLISTIC -> kineticColor
        TowerClass.ELEMENTAL_VOID -> elementalColor
        TowerClass.HEAVY_SIEGE -> siegeColor
        TowerClass.STELLAR_COSMIC -> 0xFFFFD700
    }

    fun towerStyle(cls: TowerClass): TowerDrawStyle = when (cls) {
        TowerClass.KINETIC_BALLISTIC -> kineticStyle
        TowerClass.ELEMENTAL_VOID -> elementalStyle
        TowerClass.HEAVY_SIEGE -> siegeStyle
        TowerClass.STELLAR_COSMIC -> kineticStyle
    }
}

enum class SkinTheme(val config: ThemeSkinConfig) {
    CYBERPUNK_NEON(
        ThemeSkinConfig(
            id = "cyberpunk_neon", displayName = "Cyberpunk Neon",
            description = "Glowing conduits, biome-driven ground, neon towers.",
            useBiomePalette = true, mapStyle = MapStyle.GLOW_CONDUIT,
            ground = 0xFF090E17, groundAlt = 0xFF111C2E, gridLine = 0xFF111C2E, pathBase = 0xFF1E293B,
            laneAlpha = 0xFF00F0FF, laneBeta = 0xFFFFB800, laneMerged = 0xFFC084FC,
            obstacle = 0xFF1E3A5F, clearanceMark = 0xFF1E293B,
            hudStyle = HudStyle.GLASS, hudCornerDp = 10,
            hudBackground = 0xFF070B14, hudSurface = 0xFF161F30, hudBorder = 0xFF1E293B,
            hudAccent = 0xFF00F0FF, hudText = 0xFFFFFFFF, hudTextDim = 0xFF94A3B8,
            overrideTowerColors = false,
            kineticColor = 0xFF00F0FF, elementalColor = 0xFFA855F7, siegeColor = 0xFFFF7A00,
            kineticStyle = TowerDrawStyle.NEON_GLOW, elementalStyle = TowerDrawStyle.NEON_GLOW, siegeStyle = TowerDrawStyle.NEON_GLOW,
            projectileStyle = ProjectileStyle.ORB, shockwaveColor = 0xFF00F0FF
        )
    ),
    MONOCHROME_BLUEPRINT(
        ThemeSkinConfig(
            id = "monochrome_blueprint", displayName = "Monochrome Blueprint",
            description = "Technical drawing: white linework on blueprint blue.",
            useBiomePalette = false, mapStyle = MapStyle.BLUEPRINT,
            ground = 0xFF0B2545, groundAlt = 0xFF13315C, gridLine = 0xFF3E6A9E, pathBase = 0xFF134074,
            laneAlpha = 0xFFEEF4ED, laneBeta = 0xFFB7C9E2, laneMerged = 0xFFFFFFFF,
            obstacle = 0xFF1B4079, clearanceMark = 0xFF3E6A9E,
            hudStyle = HudStyle.OUTLINE, hudCornerDp = 2,
            hudBackground = 0xFF0B2545, hudSurface = 0xFF13315C, hudBorder = 0xFF8DA9C4,
            hudAccent = 0xFFEEF4ED, hudText = 0xFFEEF4ED, hudTextDim = 0xFF8DA9C4,
            overrideTowerColors = true,
            kineticColor = 0xFFEEF4ED, elementalColor = 0xFFB7C9E2, siegeColor = 0xFF8DA9C4,
            kineticStyle = TowerDrawStyle.BLUEPRINT_LINE, elementalStyle = TowerDrawStyle.BLUEPRINT_LINE, siegeStyle = TowerDrawStyle.BLUEPRINT_LINE,
            projectileStyle = ProjectileStyle.TRACER, shockwaveColor = 0xFFEEF4ED
        )
    ),
    SOLARIZED_MILITARY(
        ThemeSkinConfig(
            id = "solarized_military", displayName = "Solarized Military",
            description = "Olive drab terrain, dirt roads, brass and rust hardware.",
            useBiomePalette = false, mapStyle = MapStyle.ROAD,
            ground = 0xFF1F2A1C, groundAlt = 0xFF2A3826, gridLine = 0xFF3C4B30, pathBase = 0xFF5C4A32,
            laneAlpha = 0xFFD9A441, laneBeta = 0xFF8FB339, laneMerged = 0xFFE8D8A0,
            obstacle = 0xFF4B5D3A, clearanceMark = 0xFF3C4B30,
            hudStyle = HudStyle.PANEL, hudCornerDp = 4,
            hudBackground = 0xFF1B2318, hudSurface = 0xFF2A3826, hudBorder = 0xFF6B7F4F,
            hudAccent = 0xFFD9A441, hudText = 0xFFEDE6C8, hudTextDim = 0xFFA3A98A,
            overrideTowerColors = true,
            kineticColor = 0xFFC8B560, elementalColor = 0xFF5FA8A0, siegeColor = 0xFFB5651D,
            kineticStyle = TowerDrawStyle.SOLID_MILITARY, elementalStyle = TowerDrawStyle.SOLID_MILITARY, siegeStyle = TowerDrawStyle.SOLID_MILITARY,
            projectileStyle = ProjectileStyle.SHELL, shockwaveColor = 0xFFD9A441
        )
    ),
    DEEP_VOID(
        ThemeSkinConfig(
            id = "deep_void", displayName = "Deep Void",
            description = "Starless dark matter ribbons with orbiting ring turrets.",
            useBiomePalette = false, mapStyle = MapStyle.VOID_RIBBON,
            ground = 0xFF05030D, groundAlt = 0xFF0B0720, gridLine = 0xFF1B1140, pathBase = 0xFF120B2E,
            laneAlpha = 0xFFA855F7, laneBeta = 0xFF22D3EE, laneMerged = 0xFFF0ABFC,
            obstacle = 0xFF2E1065, clearanceMark = 0xFF1B1140,
            hudStyle = HudStyle.VOID_GLOW, hudCornerDp = 18,
            hudBackground = 0xFF05030D, hudSurface = 0xFF120B2E, hudBorder = 0xFF4C1D95,
            hudAccent = 0xFFC084FC, hudText = 0xFFEDE9FE, hudTextDim = 0xFF8B7FB8,
            overrideTowerColors = true,
            kineticColor = 0xFF7DD3FC, elementalColor = 0xFFE879F9, siegeColor = 0xFFFB7185,
            kineticStyle = TowerDrawStyle.VOID_RING, elementalStyle = TowerDrawStyle.VOID_RING, siegeStyle = TowerDrawStyle.VOID_RING,
            projectileStyle = ProjectileStyle.DIAMOND, shockwaveColor = 0xFFC084FC
        )
    ),
    RETRO_ARCADE(
        ThemeSkinConfig(
            id = "retro_arcade", displayName = "Retro Arcade",
            description = "8-bit palette, blocky pixel tiles and square bullets.",
            useBiomePalette = false, mapStyle = MapStyle.PIXEL_TILES,
            ground = 0xFF0A0A12, groundAlt = 0xFF14142B, gridLine = 0xFF2A2A5A, pathBase = 0xFF1C1C3C,
            laneAlpha = 0xFF39FF14, laneBeta = 0xFFFF3EA5, laneMerged = 0xFFFFFF00,
            obstacle = 0xFF3A3A7A, clearanceMark = 0xFF2A2A5A,
            hudStyle = HudStyle.PIXEL, hudCornerDp = 0,
            hudBackground = 0xFF000000, hudSurface = 0xFF111122, hudBorder = 0xFF39FF14,
            hudAccent = 0xFFFFFF00, hudText = 0xFFFFFFFF, hudTextDim = 0xFF9999CC,
            overrideTowerColors = true,
            kineticColor = 0xFF39FF14, elementalColor = 0xFFFF3EA5, siegeColor = 0xFFFFFF00,
            kineticStyle = TowerDrawStyle.PIXEL_BLOCK, elementalStyle = TowerDrawStyle.PIXEL_BLOCK, siegeStyle = TowerDrawStyle.PIXEL_BLOCK,
            projectileStyle = ProjectileStyle.PIXEL, shockwaveColor = 0xFFFFFF00, shockwaveSquare = true
        )
    );

    val displayName: String get() = config.displayName

    companion object {
        fun fromId(id: String?): SkinTheme = values().firstOrNull { it.config.id == id } ?: CYBERPUNK_NEON
    }
}

/** 5 selectable visual skins for turrets (1 Default + 4 Candidates) */
enum class TurretSkinSlot(val slot: Int, val displayName: String, val description: String) {
    SLOT_1_DEFAULT(1, "Vector Cyber (Default)", "Sleek glowing vector geometry and rotary/crystal emitters"),
    SLOT_2_BLUEPRINT(2, "Monochrome Blueprint", "Technical drafting lines, measurement crosshairs, and white schematics"),
    SLOT_3_MILITARY(3, "Solarized Military", "Armored composite plating, heavy bolted rivets, and reinforced brass barrels"),
    SLOT_4_VOID(4, "Deep Void", "Ethereal dark-matter chassis, floating halo rings, and cosmic void motes"),
    SLOT_5_PIXEL(5, "Retro Pixel Arcade", "Crisp 8-bit faceted pixel geometry and high-contrast arcade borders");

    companion object {
        fun fromSlot(slot: Int): TurretSkinSlot = values().firstOrNull { it.slot == slot } ?: SLOT_1_DEFAULT
    }
}

/** 5 selectable environment themes for map grid and lane pathways (1 Default + 4 Candidates) */
enum class MapThemeSlot(val slot: Int, val displayName: String, val description: String) {
    SLOT_1_DEFAULT(1, "Cyberpunk Grid (Default)", "Dark slate terrain with glowing neon cyan/amber energy conduits"),
    SLOT_2_BLUEPRINT(2, "Blueprint Schematic", "Technical blueprint deep navy with crisp white drafting pathways"),
    SLOT_3_OUTPOST(3, "Solarized Outpost", "Tactical olive terrain with dirt-road asphalt pathways and amber hazard lines"),
    SLOT_4_VOID(4, "Void Rift", "Starless dark-matter ground with radiant violet/magenta dimensional pathways"),
    SLOT_5_MATRIX(5, "Retro Matrix", "Arcade midnight black surface with glowing green/yellow phosphor conduits");

    companion object {
        fun fromSlot(slot: Int): MapThemeSlot = values().firstOrNull { it.slot == slot } ?: SLOT_1_DEFAULT
    }
}

/** 5 selectable gunfire audio variants per archetype (1 Default + 4 Candidates) */
enum class TurretSfxSlot(val slot: Int, val kineticName: String, val elementalName: String, val siegeName: String) {
    SLOT_1_DEFAULT(1, "Rotary Gunfire (Default)", "Phase Beam (Default)", "Artillery Blast (Default)"),
    SLOT_2_VARIANT_A(2, "Heavy Autocannon", "Plasma Burn", "Sonic Detonator"),
    SLOT_3_VARIANT_B(3, "Hyper Pulse", "Ion Discharge", "Flak Burst"),
    SLOT_4_VARIANT_C(4, "Gauss Needle", "Void Pulse", "Concussion Mortar"),
    SLOT_5_VARIANT_D(5, "Rail Crack", "Arc Flare", "Plasma Howitzer");

    companion object {
        fun fromSlot(slot: Int): TurretSfxSlot = values().firstOrNull { it.slot == slot } ?: SLOT_1_DEFAULT
    }
}
