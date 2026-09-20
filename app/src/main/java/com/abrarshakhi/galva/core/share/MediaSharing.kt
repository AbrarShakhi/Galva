package com.abrarshakhi.galva.core.share

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

object MediaSharing {

    fun chooserFor(uris: List<String>, mimeTypes: List<String>): Intent? {
        if (uris.isEmpty()) return null
        val parsed = uris.map(String::toUri)
        val type = commonMimeType(mimeTypes)

        val share = if (parsed.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, parsed.first())
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    ArrayList<Uri>(parsed)
                )
        }
        share.type = type

        return Intent.createChooser(share, null).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun commonMimeType(mimeTypes: List<String>): String {
        val distinct = mimeTypes.distinct()
        distinct.singleOrNull()?.let { return it }
        val families = distinct.map { it.substringBefore('/') }.distinct()
        return families.singleOrNull()?.let { "$it/*" } ?: "*/*"
    }
}
