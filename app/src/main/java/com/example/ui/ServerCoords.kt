@file:JvmName("ServerCoordsKt")

package com.example.ui

/**
 * Maps server address → (latitude, longitude) for the planet visualization.
 * Do NOT modify Models.kt — lookup happens at runtime by address string.
 */
object ServerCoords {

    private val knownCoords = mapOf(
        // NL server — Amsterdam
        "37.220.84.106" to Pair(52.3676, 4.9041),
        "redpillcloud.ru" to Pair(52.3676, 4.9041),
        // EU server — Chisinau, Moldova
        "217.156.64.40" to Pair(47.0105, 28.8638),
        // Common fallbacks by hostname patterns
        "amsterdam" to Pair(52.3676, 4.9041),
        "nl" to Pair(52.3676, 4.9041),
        "chisinau" to Pair(47.0105, 28.8638),
        "moldova" to Pair(47.0105, 28.8638),
        "md" to Pair(47.0105, 28.8638),
        "moscow" to Pair(55.7558, 37.6173),
        "ru" to Pair(55.7558, 37.6173),
        "london" to Pair(51.5074, -0.1278),
        "uk" to Pair(51.5074, -0.1278),
        "frankfurt" to Pair(50.1109, 8.6821),
        "de" to Pair(50.1109, 8.6821),
        "paris" to Pair(48.8566, 2.3522),
        "fr" to Pair(48.8566, 2.3522),
        "newyork" to Pair(40.7128, -74.0060),
        "new-york" to Pair(40.7128, -74.0060),
        "us" to Pair(40.7128, -74.0060),
        "tokyo" to Pair(35.6762, 139.6503),
        "jp" to Pair(35.6762, 139.6503),
        "singapore" to Pair(1.3521, 103.8198),
        "sg" to Pair(1.3521, 103.8198),
        "toronto" to Pair(43.6532, -79.3832),
        "ca" to Pair(43.6532, -79.3832),
        "sydney" to Pair(-33.8688, 151.2093),
        "au" to Pair(-33.8688, 151.2093),
        "dubai" to Pair(25.2048, 55.2708),
        "ae" to Pair(25.2048, 55.2708),
        "istanbul" to Pair(41.0082, 28.9784),
        "tr" to Pair(41.0082, 28.9784),
        "helsinki" to Pair(60.1699, 24.9384),
        "fi" to Pair(60.1699, 24.9384),
        "warsaw" to Pair(52.2297, 21.0122),
        "pl" to Pair(52.2297, 21.0122)
    )

    /**
     * Resolves geographic coordinates for a given server address.
     * Returns null if the location cannot be determined.
     */
    fun resolve(address: String): Pair<Double, Double>? {
        // Exact IP match
        knownCoords[address]?.let { return it }

        // Hostname matching (case-insensitive)
        val lower = address.lowercase()
        for ((key, coords) in knownCoords) {
            if (lower.contains(key)) return coords
        }

        // Try to match server name patterns from the server list
        return null
    }

    /**
     * Also tries matching by server name if address lookup fails.
     */
    fun resolveFromServer(server: Server): Pair<Double, Double>? {
        resolve(server.address)?.let { return it }
        val nameLower = server.name.lowercase()
        for ((key, coords) in knownCoords) {
            if (nameLower.contains(key)) return coords
        }
        return null
    }
}
