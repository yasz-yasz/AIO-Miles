package com.aiomiles.app.engine

import com.aiomiles.app.data.Airport
import com.aiomiles.app.data.CabinClass
import com.aiomiles.app.data.MilesProgram
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class MilesEstimate(
    val miles: Int,
    val tierLabel: String,
    val ruleSummary: String
)

object AwardCalculator {

    fun haversineMiles(a: Airport, b: Airport): Int {
        val r = 3958.7613
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return (2 * r * atan2(sqrt(h), sqrt(1 - h))).roundToInt()
    }

    /**
     * Calculates One-Way miles needed for a given program, airline, origin, dest, distance, and cabin.
     */
    fun calculateOneWay(
        program: MilesProgram,
        airlineCode: String,
        origin: Airport,
        dest: Airport,
        distanceMi: Int,
        cabin: CabinClass,
        useKfAdvantage: Boolean = false,
        isMultiCarrierOneWorld: Boolean = false
    ): MilesEstimate {
        return when (program) {
            MilesProgram.GARUDA_MILES -> calculateGarudaMiles(airlineCode, origin, dest, distanceMi, cabin)
            MilesProgram.KRISFLYER -> calculateKrisFlyer(airlineCode, origin, dest, distanceMi, cabin, useKfAdvantage)
            MilesProgram.ASIA_MILES -> calculateAsiaMiles(airlineCode, distanceMi, cabin, isMultiCarrierOneWorld)
        }
    }

    // =========================================================================
    // 1. GARUDAMILES (Garuda Indonesia Own Metal + SkyTeam Partner Distance Chart)
    // =========================================================================
    private fun calculateGarudaMiles(
        airlineCode: String,
        origin: Airport,
        dest: Airport,
        distanceMi: Int,
        cabin: CabinClass
    ): MilesEstimate {
        val effectiveCabin = if (cabin == CabinClass.SUITES) CabinClass.FIRST else cabin
        val isOwnMetal = airlineCode == "GA"
        val isDomesticIndonesia = origin.country == "Indonesia" && dest.country == "Indonesia"

        if (isOwnMetal && isDomesticIndonesia) {
            val (y, j) = when {
                distanceMi <= 400 -> 6_500 to 13_500
                distanceMi <= 850 -> 9_500 to 19_000
                distanceMi <= 1400 -> 13_000 to 26_000
                else -> 17_500 to 35_000
            }
            val miles = when (effectiveCabin) {
                CabinClass.ECONOMY -> y
                CabinClass.PREMIUM_ECONOMY -> (y * 1.35).roundToInt()
                CabinClass.BUSINESS -> j
                CabinClass.FIRST, CabinClass.SUITES -> (j * 1.45).roundToInt()
            }
            return MilesEstimate(
                miles = roundTo500(miles),
                tierLabel = "GA Domestik",
                ruleSummary = "GarudaMiles Domestik ($distanceMi mi)"
            )
        }

        if (isOwnMetal) {
            // Garuda Indonesia International Own-Metal Chart (1-way)
            val (y, j, f) = when {
                distanceMi <= 750 -> Triple(12_500, 26_000, 42_000)      // CGK-SIN/KUL
                distanceMi <= 1500 -> Triple(17_500, 38_000, 62_000)     // CGK-BKK
                distanceMi <= 2400 -> Triple(22_000, 46_000, 75_000)     // CGK-HKG/CAN
                distanceMi <= 3600 -> Triple(29_000, 62_000, 100_000)    // CGK-PVG/ICN/NRT/SYD/MEL
                distanceMi <= 5200 -> Triple(38_000, 82_000, 130_000)    // CGK-DOH/JED/MED
                else -> Triple(49_000, 105_000, 165_000)                 // CGK-AMS
            }
            val miles = when (effectiveCabin) {
                CabinClass.ECONOMY -> y
                CabinClass.PREMIUM_ECONOMY -> roundTo500((y + j) / 2)
                CabinClass.BUSINESS -> j
                CabinClass.FIRST, CabinClass.SUITES -> f
            }
            return MilesEstimate(
                miles = miles,
                tierLabel = "GA Internasional",
                ruleSummary = "GarudaMiles Own-Metal ($distanceMi mi)"
            )
        }

        // SkyTeam & Etihad Partner Award Chart (Distance Bands, 1-way)
        val band = skyTeamBand(distanceMi)
        val miles = when (effectiveCabin) {
            CabinClass.ECONOMY -> band.y
            CabinClass.PREMIUM_ECONOMY -> band.w
            CabinClass.BUSINESS -> band.j
            CabinClass.FIRST, CabinClass.SUITES -> band.f
        }
        val label = if (airlineCode == "EY") "Mitra Etihad" else "SkyTeam Partner"
        return MilesEstimate(
            miles = miles,
            tierLabel = label,
            ruleSummary = "GarudaMiles $label Band ${band.label} ($distanceMi mi)"
        )
    }

    private data class DistanceBand(val label: String, val y: Int, val w: Int, val j: Int, val f: Int)

    private fun skyTeamBand(dist: Int): DistanceBand = when {
        dist <= 500 -> DistanceBand("≤500 mi", 10_000, 15_000, 20_000, 30_000)
        dist <= 1000 -> DistanceBand("501-1.000 mi", 15_000, 22_500, 30_000, 45_000)
        dist <= 1500 -> DistanceBand("1.001-1.500 mi", 20_000, 30_000, 40_000, 60_000)
        dist <= 2500 -> DistanceBand("1.501-2.500 mi", 27_500, 41_000, 55_000, 82_500)
        dist <= 3500 -> DistanceBand("2.501-3.500 mi", 35_000, 52_500, 70_000, 105_000)
        dist <= 5000 -> DistanceBand("3.501-5.000 mi", 45_000, 67_500, 90_000, 135_000)
        dist <= 7000 -> DistanceBand("5.001-7.000 mi", 55_000, 82_500, 110_000, 165_000)
        dist <= 9000 -> DistanceBand("7.001-9.000 mi", 65_000, 97_500, 130_000, 195_000)
        else -> DistanceBand("9.001+ mi", 80_000, 120_000, 160_000, 240_000)
    }

    // =========================================================================
    // 2. KRISFLYER (Singapore Airlines Zone Chart Saver/Advantage + Star Alliance)
    // =========================================================================
    private fun calculateKrisFlyer(
        airlineCode: String,
        origin: Airport,
        dest: Airport,
        distanceMi: Int,
        cabin: CabinClass,
        useAdvantage: Boolean
    ): MilesEstimate {
        if (airlineCode == "TR") {
            // Scoot KrisFlyer distance-equivalent redemption
            val baseY = max(4_500, roundTo500((distanceMi * 5.2).roundToInt()))
            val miles = if (cabin == CabinClass.PREMIUM_ECONOMY) roundTo500((baseY * 1.65).roundToInt()) else baseY
            return MilesEstimate(
                miles = miles,
                tierLabel = "Scoot Award",
                ruleSummary = "KrisFlyer Scoot ($distanceMi mi)"
            )
        }

        val isSQ = airlineCode == "SQ"
        val z1 = min(origin.kfZone, dest.kfZone)
        val z2 = max(origin.kfZone, dest.kfZone)

        if (isSQ) {
            val (y, w, j, f) = sqSaverRates(z1, z2, distanceMi)
            val base = when (cabin) {
                CabinClass.ECONOMY -> y
                CabinClass.PREMIUM_ECONOMY -> w
                CabinClass.BUSINESS -> j
                CabinClass.FIRST, CabinClass.SUITES -> f
            }
            val finalMiles = if (useAdvantage) sqAdvantageMultiplier(base, cabin) else base
            val tier = when {
                cabin == CabinClass.SUITES && useAdvantage -> "SQ Suites Advantage"
                cabin == CabinClass.SUITES -> "SQ Suites Saver"
                useAdvantage -> "SQ Advantage"
                else -> "SQ Saver"
            }
            return MilesEstimate(
                miles = finalMiles,
                tierLabel = tier,
                ruleSummary = "KrisFlyer $tier (Zona $z1 ↔ Zona $z2)"
            )
        } else {
            // Star Alliance & Bilateral Partner Chart (1-way)
            val (y, w, j, f) = starPartnerRates(z1, z2, distanceMi)
            val miles = when (cabin) {
                CabinClass.ECONOMY -> y
                CabinClass.PREMIUM_ECONOMY -> w
                CabinClass.BUSINESS -> j
                CabinClass.FIRST, CabinClass.SUITES -> f
            }
            val label = if (airlineCode in setOf("VA", "B6", "HO")) "Mitra KrisFlyer" else "Star Alliance"
            return MilesEstimate(
                miles = miles,
                tierLabel = label,
                ruleSummary = "KrisFlyer $label (Zona $z1 ↔ Zona $z2)"
            )
        }
    }

    private data class Quad(val y: Int, val w: Int, val j: Int, val f: Int)

    private fun sqAdvantageMultiplier(saver: Int, cabin: CabinClass): Int {
        val factor = when (cabin) {
            CabinClass.ECONOMY -> 1.65
            CabinClass.PREMIUM_ECONOMY -> 1.45
            CabinClass.BUSINESS -> 1.38
            CabinClass.FIRST, CabinClass.SUITES -> 1.60
        }
        return roundTo500((saver * factor).roundToInt())
    }

    // Authentic Singapore Airlines KrisFlyer Saver 1-way table (Zone 1..13)
    private fun sqSaverRates(z1: Int, z2: Int, dist: Int): Quad {
        if (z1 == 1) { // From/To Singapore
            return when (z2) {
                1, 2 -> Quad(8_500, 14_500, 21_000, 30_500)      // SIN <-> ID/MY/BN
                3 -> Quad(13_500, 20_500, 24_000, 33_500)        // SIN <-> TH/VN/PH
                4 -> Quad(16_500, 26_500, 34_000, 45_000)        // SIN <-> HKG/TPE/South China
                5 -> Quad(21_500, 34_500, 43_000, 58_500)        // SIN <-> PVG/PEK
                6 -> Quad(20_000, 34_500, 43_000, 58_500)        // SIN <-> India/South Asia
                7 -> Quad(27_000, 43_000, 52_000, 77_000)        // SIN <-> Japan/Korea
                8 -> Quad(21_500, 34_500, 40_500, 58_500)        // SIN <-> Perth/Darwin
                9 -> Quad(30_500, 51_000, 68_500, 93_500)        // SIN <-> SYD/MEL/AKL
                10 -> Quad(29_000, 46_500, 56_500, 85_000)       // SIN <-> Middle East/Africa
                11 -> Quad(42_000, 71_000, 103_500, 141_000)     // SIN <-> Europe
                12 -> Quad(42_000, 75_000, 107_000, 148_500)     // SIN <-> US West Coast
                else -> Quad(44_000, 80_500, 111_500, 148_500)   // SIN <-> US East Coast
            }
        }
        if (z1 == 2) { // From/To Indonesia / Malaysia / Brunei via SIN
            return when (z2) {
                2 -> Quad(10_500, 17_000, 23_000, 33_500)
                3 -> Quad(13_500, 20_500, 24_000, 33_500)
                4 -> Quad(16_500, 26_500, 34_000, 45_000)
                5 -> Quad(21_500, 34_500, 43_000, 58_500)
                6 -> Quad(20_000, 34_500, 43_000, 58_500)
                7 -> Quad(27_000, 43_000, 52_000, 77_000)
                8 -> Quad(21_500, 34_500, 40_500, 58_500)
                9 -> Quad(30_500, 51_000, 68_500, 93_500)
                10 -> Quad(29_000, 46_500, 56_500, 85_000)
                11 -> Quad(42_000, 71_000, 103_500, 141_000)
                12 -> Quad(44_000, 75_000, 107_000, 148_500)
                else -> Quad(44_000, 80_500, 111_500, 148_500)
            }
        }
        // SQ Fifth Freedom & Inter-zone routes (e.g., FRA-JFK, NRT-LAX, MAN-IAH, MXP-BCN)
        if (z1 == 11 && z2 == 13) return Quad(25_000, 52_000, 81_000, 97_000) // FRA-JFK / MAN-IAH
        if (z1 == 7 && z2 == 12) return Quad(38_500, 73_000, 103_500, 120_500) // NRT-LAX
        if (z1 == 11 && z2 == 11) return Quad(12_500, 20_000, 25_000, 38_000)  // MXP-BCN

        // Fallback based on distance for other zone pairs
        return when {
            dist <= 1000 -> Quad(12_500, 20_000, 25_000, 38_000)
            dist <= 2500 -> Quad(20_000, 34_000, 43_000, 58_500)
            dist <= 4500 -> Quad(30_500, 51_000, 68_500, 93_500)
            dist <= 7000 -> Quad(42_000, 71_000, 103_500, 141_000)
            else -> Quad(44_000, 80_500, 111_500, 148_500)
        }
    }

    private fun starPartnerRates(z1: Int, z2: Int, dist: Int): Quad {
        // Same zone domestic/intra-regional
        if (z1 == z2) {
            return when (z1) {
                1, 2, 3 -> Quad(13_500, 20_000, 25_500, 38_500)
                4, 5, 7 -> Quad(13_500, 21_500, 29_000, 43_000)
                11 -> Quad(13_500, 21_000, 29_000, 45_000)
                12, 13 -> Quad(14_000, 22_000, 26_000, 40_000)
                else -> Quad(15_000, 23_000, 31_000, 46_000)
            }
        }
        // Intra SE Asia
        if (z1 in 1..3 && z2 in 1..3) return Quad(13_500, 20_000, 25_500, 38_500)
        // SE Asia <-> HK/TW/China
        if (z1 in 1..3 && z2 in 4..5) return Quad(22_500, 35_000, 46_000, 64_000)
        // SE Asia <-> South Asia
        if (z1 in 1..3 && z2 == 6) return Quad(23_500, 36_000, 49_000, 66_000)
        // SE Asia <-> Japan / South Korea
        if (z1 in 1..3 && z2 == 7) return Quad(28_500, 44_000, 56_000, 82_000)
        // SE Asia <-> Australia / NZ
        if (z1 in 1..3 && z2 in 8..9) return Quad(32_000, 54_000, 72_500, 99_000)
        // SE Asia <-> Middle East / Africa
        if (z1 in 1..3 && z2 == 10) return Quad(31_500, 50_000, 63_000, 92_000)
        // SE Asia <-> Europe
        if (z1 in 1..3 && z2 == 11) return Quad(46_000, 76_000, 108_500, 152_000)
        // SE Asia <-> North America / LatAm
        if (z1 in 1..3 && z2 in 12..13) return Quad(51_000, 84_000, 121_500, 165_000)
        // East Asia <-> Europe
        if (z1 in 4..7 && z2 == 11) return Quad(44_500, 74_000, 105_000, 146_000)
        // East Asia <-> North America
        if (z1 in 4..7 && z2 in 12..13) return Quad(47_000, 79_000, 114_000, 154_000)
        // Europe <-> North America
        if (z1 == 11 && z2 in 12..13) return Quad(30_500, 56_000, 81_000, 115_000)
        // Middle East/Africa <-> Europe
        if (z1 == 10 && z2 == 11) return Quad(26_000, 42_000, 55_000, 82_000)
        // Australia/NZ <-> North America
        if (z1 in 8..9 && z2 in 12..13) return Quad(45_000, 76_000, 112_000, 150_000)

        return when {
            dist <= 1200 -> Quad(15_000, 23_000, 30_000, 45_000)
            dist <= 2800 -> Quad(24_000, 38_000, 50_000, 70_000)
            dist <= 5000 -> Quad(34_000, 55_000, 75_000, 108_000)
            dist <= 7500 -> Quad(46_000, 76_000, 108_500, 152_000)
            else -> Quad(52_000, 85_000, 122_000, 168_000)
        }
    }

    // =========================================================================
    // 3. ASIA MILES (Cathay Pacific Standard + oneworld Partner + Multi-Carrier)
    // =========================================================================
    private fun calculateAsiaMiles(
        airlineCode: String,
        distanceMi: Int,
        cabin: CabinClass,
        isMultiCarrier: Boolean
    ): MilesEstimate {
        val effectiveCabin = if (cabin == CabinClass.SUITES) CabinClass.FIRST else cabin

        if (isMultiCarrier) {
            val band = oneworldMultiCarrierBand(distanceMi)
            val miles = when (effectiveCabin) {
                CabinClass.ECONOMY -> band.y
                CabinClass.PREMIUM_ECONOMY -> band.w
                CabinClass.BUSINESS -> band.j
                CabinClass.FIRST, CabinClass.SUITES -> band.f
            }
            return MilesEstimate(
                miles = miles,
                tierLabel = "oneworld Multi-Carrier",
                ruleSummary = "Asia Miles Multi-Carrier ${band.label} ($distanceMi mi)"
            )
        }

        val isCX = airlineCode == "CX"
        val band = if (isCX) cxDistanceBand(distanceMi) else oneworldPartnerBand(distanceMi)
        val miles = when (effectiveCabin) {
            CabinClass.ECONOMY -> band.y
            CabinClass.PREMIUM_ECONOMY -> band.w
            CabinClass.BUSINESS -> band.j
            CabinClass.FIRST, CabinClass.SUITES -> band.f
        }
        val label = when {
            isCX -> "CX Standard"
            airlineCode in setOf("LA", "GF", "PG") -> "Mitra Asia Miles"
            else -> "oneworld Partner"
        }
        return MilesEstimate(
            miles = miles,
            tierLabel = label,
            ruleSummary = "Asia Miles $label ${band.label} ($distanceMi mi)"
        )
    }

    private fun cxDistanceBand(dist: Int): DistanceBand = when {
        dist <= 750 -> DistanceBand("Ultra-Short ≤750 mi", 7_500, 12_000, 16_000, 25_000)
        dist <= 2750 -> DistanceBand("Short 751-2.750 mi", 12_500, 21_000, 28_000, 45_000)
        dist <= 5000 -> DistanceBand("Medium 2.751-5.000 mi", 20_000, 34_000, 58_000, 87_000)
        dist <= 7500 -> DistanceBand("Long 5.001-7.500 mi", 27_000, 50_000, 84_000, 125_000)
        else -> DistanceBand("Ultra-Long 7.501+ mi", 38_000, 75_000, 110_000, 160_000)
    }

    private fun oneworldPartnerBand(dist: Int): DistanceBand = when {
        dist <= 750 -> DistanceBand("Ultra-Short ≤750 mi", 9_000, 14_000, 19_000, 30_000)
        dist <= 2750 -> DistanceBand("Short 751-2.750 mi", 14_000, 24_000, 33_000, 52_000)
        dist <= 5000 -> DistanceBand("Medium 2.751-5.000 mi", 23_000, 39_000, 63_000, 95_000)
        dist <= 7500 -> DistanceBand("Long 5.001-7.500 mi", 30_000, 56_000, 89_000, 135_000)
        else -> DistanceBand("Ultra-Long 7.501+ mi", 42_000, 82_000, 115_000, 175_000)
    }

    private fun oneworldMultiCarrierBand(dist: Int): DistanceBand = when {
        dist <= 2750 -> DistanceBand("≤2.750 mi", 20_000, 32_000, 45_000, 68_000)
        dist <= 5000 -> DistanceBand("2.751-5.000 mi", 30_000, 50_000, 72_000, 108_000)
        dist <= 7500 -> DistanceBand("5.001-7.500 mi", 40_000, 68_000, 96_000, 145_000)
        dist <= 10000 -> DistanceBand("7.501-10.000 mi", 50_000, 88_000, 125_000, 190_000)
        dist <= 14000 -> DistanceBand("10.001-14.000 mi", 62_000, 110_000, 155_000, 235_000)
        else -> DistanceBand("14.001+ mi", 80_000, 140_000, 200_000, 295_000)
    }

    private fun roundTo500(value: Int): Int {
        return ((value + 250) / 500) * 500
    }
}
