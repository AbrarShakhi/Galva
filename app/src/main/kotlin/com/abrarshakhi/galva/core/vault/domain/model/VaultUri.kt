package com.abrarshakhi.galva.core.vault.domain.model

/**
 * `galva-vault://` addresses for vault content.
 *
 * Vault items travel through the UI as `MediaItem`s so the shared grid and viewer can render them.
 * These URIs take the place of a MediaStore URI there, and only the vault's own Coil fetcher and
 * Media3 data source can resolve them — so a vault item handed to share or MediaStore delete by
 * mistake fails instead of leaking.
 */
object VaultUri {

    const val SCHEME = "galva-vault"

    enum class Kind(val host: String) {
        /** The small encrypted JPEG made at import, for grid cells. */
        Thumbnail("thumb"),

        /** The whole original, decoded as an image. */
        Full("full"),

        /** The whole original as a byte stream, for video playback. */
        Blob("blob"),
    }

    data class Ref(val kind: Kind, val id: Long)

    fun thumbnail(id: Long): String = build(Kind.Thumbnail, id)

    fun full(id: Long): String = build(Kind.Full, id)

    fun blob(id: Long): String = build(Kind.Blob, id)

    fun parse(uri: String): Ref? {
        val rest = uri.removePrefix("$SCHEME://").takeIf { it != uri } ?: return null
        val host = rest.substringBefore('/')
        val kind = Kind.entries.firstOrNull { it.host == host } ?: return null
        val hex = rest.substringAfter('/', missingDelimiterValue = "")
        val id = VaultIds.fromHex(hex) ?: return null
        return Ref(kind, id)
    }

    private fun build(kind: Kind, id: Long): String = "$SCHEME://${kind.host}/${VaultIds.toHex(id)}"
}
