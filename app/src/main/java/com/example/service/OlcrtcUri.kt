package com.example.service

/**
 * Parser for the olcrtc:// compact URI convention (v1), see
 * github.com/openlibrecommunity/olcrtc docs/uri.md.
 *
 * Format:
 *   olcrtc://<Provider>?<Transport><key=value&key=value>@<RoomID>#<EncryptionKey>$<MIMO>
 * The <...> payload block is dropped when the transport needs no parameters.
 */
data class OlcrtcSpec(
    val uri: String,
    val provider: String,
    val transport: String,
    val room: String,
    val key: String,
    val label: String,
    val vp8Fps: Int = 30,
    val vp8Batch: Int = 64
)

object OlcrtcUri {

    fun parse(raw: String): OlcrtcSpec? {
        val uri = raw.trim()
        if (!uri.startsWith("olcrtc://")) return null

        // label (MIMO) after the last '$'
        val dollarIdx = uri.lastIndexOf('$')
        val label = if (dollarIdx >= 0) uri.substring(dollarIdx + 1).trim() else ""

        var rest = if (dollarIdx >= 0) uri.substring(0, dollarIdx) else uri
        rest = rest.removePrefix("olcrtc://")

        // '#' separates the room from the key
        val hashIdx = rest.indexOf('#')
        if (hashIdx < 0) return null
        val key = rest.substring(hashIdx + 1).trim()
        rest = rest.substring(0, hashIdx)

        // '@' separates the transport block from the room id
        val atIdx = rest.indexOf('@')
        if (atIdx < 0) return null
        val transportBlock = rest.substring(0, atIdx)  // e.g. "telemost?vp8channel<...>"
        val room = rest.substring(atIdx + 1).trim()

        // provider is before the '?' that precedes the transport name
        val provider = transportBlock.substringBefore('?').trim().lowercase()

        // transport part: everything after '?', e.g. "vp8channel<vp8-fps=30&vp8-batch=64>"
        val transportPart = transportBlock.substringAfter('?').trim()
        if (transportPart.isEmpty()) return null

        val params: Map<String, String>
        val transportName: String
        val openIdx = transportPart.indexOf('<')
        val closeIdx = transportPart.lastIndexOf('>')
        if (openIdx >= 0 && closeIdx > openIdx) {
            transportName = transportPart.substring(0, openIdx).trim().lowercase()
            params = transportPart.substring(openIdx + 1, closeIdx)
                .split("&")
                .mapNotNull { pair ->
                    val kv = pair.split("=", limit = 2)
                    val k = kv[0].trim()
                    if (k.isBlank()) null else k to kv.getOrElse(1) { "" }.trim()
                }
                .toMap()
        } else {
            transportName = transportPart.trim().lowercase()
            params = emptyMap()
        }

        if (provider.isEmpty() || transportName.isEmpty() || room.isEmpty() || key.isEmpty()) return null

        val vp8Fps = params["vp8-fps"]?.toIntOrNull()?.coerceIn(1, 120) ?: 30
        val vp8Batch = params["vp8-batch"]?.toIntOrNull()?.takeIf { it > 0 } ?: 64

        return OlcrtcSpec(
            uri = uri,
            provider = provider,
            transport = transportName,
            room = room,
            key = key,
            label = label,
            vp8Fps = vp8Fps,
            vp8Batch = vp8Batch
        )
    }

    fun isOlcrtcUri(uri: String): Boolean = uri.trimStart().startsWith("olcrtc://")
}
