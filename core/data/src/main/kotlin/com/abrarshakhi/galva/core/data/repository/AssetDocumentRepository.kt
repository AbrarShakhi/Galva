package com.abrarshakhi.galva.core.data.repository

import android.content.Context
import com.abrarshakhi.galva.core.model.AppDocument
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal class AssetDocumentRepository(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher,
) : DocumentRepository {

    override suspend fun read(document: AppDocument): String = withContext(dispatcher) {
        context.assets.open("$DOCUMENTS_DIRECTORY/${document.fileName}").bufferedReader().use { it.readText() }
    }
}

internal const val DOCUMENTS_DIRECTORY = "documents"
