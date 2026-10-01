package com.abrarshakhi.galva.core.media.data.local

import androidx.room.TypeConverter
import com.abrarshakhi.galva.core.media.domain.model.MediaType

class Converters {

    @TypeConverter
    fun mediaTypeToName(type: MediaType): String = type.name

    @TypeConverter
    fun nameToMediaType(name: String): MediaType = MediaType.valueOf(name)
}
