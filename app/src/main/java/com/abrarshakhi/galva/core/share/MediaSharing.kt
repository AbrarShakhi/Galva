package com.abrarshakhi.galva.core.share

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

/**
 * Builds a share sheet for MediaStore items.
 *
 * MediaStore content URIs are already shareable — granting read permission on the intent is enough
 * and no FileProvider copy is needed, so nothing is written to disk to share a photo.
 */
object MediaSharing {

    fun chooserFor(uris: List<String>, mimeTypes: List<String>): Intent? {
        if (uris.isEmpty()) return null
        val parsed = uris.map(String::toUri)
        val type = commonMimeType(mimeTypes)

        val share = if (parsed.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, parsed.first())
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE)
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(parsed))
        }
        share.type = type

        return Intent.createChooser(share, null)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    /**
     * Narrowest type that still covers the selection: an exact match when every item agrees, a
     * family wildcard for a same-family mix, and a full wildcard once both families are present.
     */
    private fun commonMimeType(mimeTypes: List<String>): String {
        val distinct = mimeTypes.distinct()
        distinct.singleOrNull()?.let { return it }
        val families = distinct.map { it.substringBefore('/') }.distinct()
        return families.singleOrNull()?.let { "$it/*" } ?: "*/*"
    }
}
