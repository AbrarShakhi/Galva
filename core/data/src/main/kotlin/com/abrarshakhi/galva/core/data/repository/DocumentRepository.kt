package com.abrarshakhi.galva.core.data.repository

import com.abrarshakhi.galva.core.model.AppDocument

interface DocumentRepository {
    suspend fun read(document: AppDocument): String
}
