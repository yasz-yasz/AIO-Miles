package com.aiomiles.app.data

enum class CabinClass(val code: String, val label: String, val shortLabel: String) {
    ECONOMY("Y", "Economy", "Eco"),
    PREMIUM_ECONOMY("W", "Premium Economy", "Prem Eco"),
    BUSINESS("J", "Business", "Biz"),
    ARIA_SUITE("A", "Aria Suite (CX)", "Aria"),
    FIRST("F", "First Class", "First"),
    SUITES("S", "Suites (A380)", "Suites");

    companion object {
        fun fromCode(code: String): CabinClass =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ECONOMY
    }
}

enum class MilesProgram(
    val displayName: String,
    val allianceName: String,
    val shortCode: String,
    val colorHex: Long
) {
    GARUDA_MILES("GarudaMiles", "SkyTeam", "GM", 0xFF00778B),
    KRISFLYER("KrisFlyer", "Star Alliance", "KF", 0xFF1B365D),
    ASIA_MILES("Asia Miles", "oneworld", "AM", 0xFF006564);

    companion object {
        fun fromString(raw: String): MilesProgram = when {
            raw.contains("Garuda", ignoreCase = true) -> GARUDA_MILES
            raw.contains("Kris", ignoreCase = true) -> KRISFLYER
            else -> ASIA_MILES
        }
    }
}

data class Airport(
    val iata: String,
    val city: String,
    val country: String,
    val kfZone: Int,
    val lat: Double,
    val lon: Double
) {
    val displayLabel: String get() = "$iata — $city, $country"
}

data class Airline(
    val code: String,
    val name: String,
    val alliance: String,
    val program: MilesProgram,
    val hubs: List<String>,
    val cabins: Set<String>,
    val country: String,
    val note: String
)

data class FlightRoute(
    val origin: String,
    val dest: String,
    val airlineCode: String,
    val alliance: String,
    val program: MilesProgram,
    val distanceMi: Int,
    val cabins: Set<String>,
    val isFifthFreedom: Boolean
) {
    val distanceKm: Int get() = (distanceMi * 1.60934).toInt()
}

data class LegPlan(
    val route: FlightRoute,
    val airlineName: String,
    val originCity: String,
    val destCity: String,
    val program: MilesProgram,
    val miles: Int,
    val tierLabel: String,
    val cabinUsed: CabinClass
)

enum class ItineraryType(val label: String) {
    DIRECT("Direct (Langsung)"),
    SAME_ALLIANCE_SINGLE("1-Stop Sama Aliansi (1 PNR)"),
    SAME_ALLIANCE_SPLIT("1-Stop Sama Aliansi (Split)"),
    CROSS_ALLIANCE_MIX("Mix Miles Beda Aliansi")
}

data class ItineraryRecommendation(
    val id: String,
    val type: ItineraryType,
    val legs: List<LegPlan>,
    val totalMiles: Int,
    val totalDistanceMi: Int,
    val milesByProgram: Map<MilesProgram, Int>,
    val highlightBadge: String,
    val explanation: String,
    val transferHub: String?
)

data class MilesWallet(
    val garudaMiles: Int = 0,
    val krisFlyer: Int = 0,
    val asiaMiles: Int = 0
) {
    fun balanceFor(program: MilesProgram): Int = when (program) {
        MilesProgram.GARUDA_MILES -> garudaMiles
        MilesProgram.KRISFLYER -> krisFlyer
        MilesProgram.ASIA_MILES -> asiaMiles
    }

    fun hasAnyBalance(): Boolean = garudaMiles > 0 || krisFlyer > 0 || asiaMiles > 0

    fun canAfford(required: Map<MilesProgram, Int>): Boolean {
        if (!hasAnyBalance()) return false
        return required.all { (prog, need) -> balanceFor(prog) >= need }
    }

    fun deficitSummary(required: Map<MilesProgram, Int>): String {
        val deficits = required.mapNotNull { (prog, need) ->
            val diff = need - balanceFor(prog)
            if (diff > 0) "${prog.shortCode} kurang ${formatMiles(diff)}" else null
        }
        return if (deficits.isEmpty()) "Saldo cukup!" else deficits.joinToString(" • ")
    }
}

fun formatMiles(miles: Int): String {
    val s = miles.toString()
    val sb = StringBuilder()
    var count = 0
    for (i in s.length - 1 downTo 0) {
        sb.append(s[i])
        count++
        if (count % 3 == 0 && i != 0) {
            sb.append('.')
        }
    }
    return sb.reverse().toString()
}
