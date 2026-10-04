package com.abrarshakhi.galva.core.vault

object VaultUri {

    const val SCHEME = "galva-vault"

    enum class Kind(val host: String) {
        Thumbnail("thumb"),

        Full("full"),

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
