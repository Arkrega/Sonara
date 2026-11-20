package com.sonara.music.eq.data

enum class ReverbPresetType(val id: Int) {
    SMALL_ROOM(0),
    MEDIUM_ROOM(1),
    LARGE_HALL(2),
    CATHEDRAL(3),
    PLATE(4),
    STUDIO(5),
    CUSTOM(6);

    companion object {
        fun fromId(id: Int): ReverbPresetType =
            entries.firstOrNull { it.id == id } ?: SMALL_ROOM
    }
}

data class ReverbConfig(
    val enabled: Boolean = false,
    val presetType: ReverbPresetType = ReverbPresetType.SMALL_ROOM,
    val roomSize: Float = 0.35f,
    val damping: Float = 0.50f,
    val wetLevel: Float = 0.25f,
    val dryLevel: Float = 0.95f,
    val width: Float = 0.70f,
    val decay: Float = 0.30f,
) {
    companion object {
        val SMALL_ROOM = ReverbConfig(
            presetType = ReverbPresetType.SMALL_ROOM,
            roomSize = 0.35f,
            damping = 0.50f,
            wetLevel = 0.25f,
            dryLevel = 0.95f,
            width = 0.70f,
            decay = 0.30f,
        )
        val MEDIUM_ROOM = ReverbConfig(
            presetType = ReverbPresetType.MEDIUM_ROOM,
            roomSize = 0.55f,
            damping = 0.40f,
            wetLevel = 0.35f,
            dryLevel = 0.90f,
            width = 0.85f,
            decay = 0.50f,
        )
        val LARGE_HALL = ReverbConfig(
            presetType = ReverbPresetType.LARGE_HALL,
            roomSize = 0.80f,
            damping = 0.25f,
            wetLevel = 0.45f,
            dryLevel = 0.85f,
            width = 1.00f,
            decay = 0.75f,
        )
        val CATHEDRAL = ReverbConfig(
            presetType = ReverbPresetType.CATHEDRAL,
            roomSize = 0.92f,
            damping = 0.15f,
            wetLevel = 0.55f,
            dryLevel = 0.75f,
            width = 1.00f,
            decay = 0.90f,
        )
        val PLATE = ReverbConfig(
            presetType = ReverbPresetType.PLATE,
            roomSize = 0.65f,
            damping = 0.10f,
            wetLevel = 0.40f,
            dryLevel = 0.88f,
            width = 0.90f,
            decay = 0.60f,
        )
        val STUDIO = ReverbConfig(
            presetType = ReverbPresetType.STUDIO,
            roomSize = 0.25f,
            damping = 0.70f,
            wetLevel = 0.20f,
            dryLevel = 1.00f,
            width = 0.60f,
            decay = 0.20f,
        )

        fun forType(type: ReverbPresetType): ReverbConfig = when (type) {
            ReverbPresetType.SMALL_ROOM -> SMALL_ROOM
            ReverbPresetType.MEDIUM_ROOM -> MEDIUM_ROOM
            ReverbPresetType.LARGE_HALL -> LARGE_HALL
            ReverbPresetType.CATHEDRAL -> CATHEDRAL
            ReverbPresetType.PLATE -> PLATE
            ReverbPresetType.STUDIO -> STUDIO
            ReverbPresetType.CUSTOM -> SMALL_ROOM.copy(presetType = ReverbPresetType.CUSTOM)
        }
    }
}
