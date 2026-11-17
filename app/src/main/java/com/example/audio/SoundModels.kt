package com.example.audio

/** Procedurally synthesised sound effects. No bundled audio assets are required. */
enum class SoundFx(val label: String, val minIntervalMs: Long) {
    PLACE("Placement Thud", 40L),
    UPGRADE("Upgrade Chime", 80L),
    FIRE_KINETIC("Kinetic Fire (Default)", 65L),
    FIRE_ELEMENTAL("Elemental Fire (Default)", 80L),
    FIRE_SIEGE("Siege Fire (Default)", 110L),
    FIRE_STELLAR("Stellar Cosmic Fire", 75L),
    INVALID("Invalid Tap Buzz", 120L),
    SELL("Salvage Scrap Return", 60L)
}

enum class SoundProfile(val label: String) {
    CYBER_SYNTH("Cyber Synth (Default)"),
    RETRO_8BIT("Retro 8-Bit Arcade"),
    DEEP_ACOUSTIC("Heavy Acoustic Impact"),
    SCI_FI_LASER("Sci-Fi Laser Grid")
}

/**
 * 5-Track Music Selection plus Rolling Cyclical Playlist and Mute.
 */
enum class MusicTrack(val label: String) {
    NONE("Mute Music"),
    ROLLING_PLAYLIST("Rolling Playlist (Cycles 1-5)"),
    CYBER_AMBIENT("Track 1: Sector Zero Synth"),
    PULSE_ARPEGGIO("Track 2: Dual Front Pulse"),
    DEEP_SPACE("Track 3: Sub-Void Drone"),
    NEON_OVERDRIVE("Track 4: Neon Overdrive"),
    CELESTIAL_BASTION("Track 5: Celestial Bastion")
}

/**
 * Procedural Asset Record for Sound and Skin Library.
 * Supports tri-state Like/Dislike voting (voteState), Star bookmarks, equipping, and archive paths.
 */
data class CustomAssetRecord(
    val id: String,
    val type: String, // "SKIN" or "SOUND"
    val name: String,
    val category: String,
    var isFavoriteHeart: Boolean = false,
    var isFavoriteStar: Boolean = false,
    val unlockScore: Int = 0,
    val metadata: String = "",
    val towerClassOrdinal: Int = -1,
    val slotIndex: Int = 1,
    val filePath: String? = null,
    var voteState: Int = 0, // 1 = Liked, -1 = Disliked, 0 = Neutral
    var isEquipped: Boolean = false,
    var updatedAtEpochMs: Long = 0L
)
