package com.abrarshakhi.galva.core.database

import androidx.room.TypeConverter
import com.abrarshakhi.galva.core.model.MediaType

class Converters {

    @TypeConverter
    fun mediaTypeToName(type: MediaType): String = type.name

    @TypeConverter
    fun nameToMediaType(name: String): MediaType = MediaType.valueOf(name)
}
