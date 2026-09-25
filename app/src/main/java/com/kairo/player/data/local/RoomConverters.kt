package com.kairo.player.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomConverters {
    @TypeConverter
    fun encodeStringList(values: List<String>): String = Json.encodeToString(values)

    @TypeConverter
    fun decodeStringList(value: String): List<String> = Json.decodeFromString(value)
}