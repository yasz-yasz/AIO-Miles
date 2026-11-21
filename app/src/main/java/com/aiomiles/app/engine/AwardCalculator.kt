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
     * Updated with 2026 award chart revisions for GarudaMiles, KrisFlyer (eff. Nov 2025/2026), and Asia Miles (eff. May 2026).
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
            MilesProgram.ASIA_MILES -> calculateAsiaMiles(airlineCode, origin, dest, distanceMi, cabin, isMultiCarrierOneWorld)
        }
    }

    // =========================================================================
    // 1. GARUDAMILES (Tabel Penukaran Miles GA start APR 2026 + SkyTeam/Partner)
    // =========================================================================
    private data class GaOwnMetalRate(val y: Int, val j: Int, val f: Int)

    private val gaDomesticRouteRates: Map<Set<String>, GaOwnMetalRate> = mapOf(
        // Jakarta (CGK / HLP) routes
        setOf("CGK", "TKG") to GaOwnMetalRate(4_800, 13_000, 25_400),
        setOf("CGK", "SRG") to GaOwnMetalRate(9_600, 15_400, 25_400),
        setOf("CGK", "PLM") to GaOwnMetalRate(9_600, 15_400, 25_400),
        setOf("CGK", "YIA") to GaOwnMetalRate(10_500, 17_000, 25_400),
        setOf("CGK", "SOC") to GaOwnMetalRate(9_600, 15_400, 25_400),
        setOf("CGK", "BKS") to GaOwnMetalRate(8_600, 15_400, 25_400),
        setOf("CGK", "DJB") to GaOwnMetalRate(9_600, 15_400, 25_400),
        setOf("CGK", "SUB") to GaOwnMetalRate(11_500, 19_000, 25_400),
        setOf("CGK", "MLG") to GaOwnMetalRate(9_600, 15_400, 25_400),
        setOf("CGK", "PGK") to GaOwnMetalRate(9_600, 15_400, 25_400),
        setOf("CGK", "PNK") to GaOwnMetalRate(9_600, 15_400, 25_400),
        setOf("CGK", "TNJ") to GaOwnMetalRate(15_000, 24_500, 45_300),
        setOf("CGK", "BTH") to GaOwnMetalRate(16_800, 27_500, 45_300),
        setOf("CGK", "PKY") to GaOwnMetalRate(16_800, 24_700, 45_300),
        setOf("CGK", "PDG") to GaOwnMetalRate(16_800, 27_500, 45_300),
        setOf("CGK", "BDJ") to GaOwnMetalRate(16_800, 27_500, 45_300),
        setOf("CGK", "PKU") to GaOwnMetalRate(16_800, 27_500, 45_300),
        setOf("CGK", "DPS") to GaOwnMetalRate(17_900, 32_000, 45_300),
        setOf("CGK", "LOP") to GaOwnMetalRate(16_800, 27_500, 45_300),
        setOf("CGK", "BPN") to GaOwnMetalRate(21_600, 35_200, 58_000),
        setOf("CGK", "AAP") to GaOwnMetalRate(21_600, 35_200, 58_000),
        setOf("CGK", "KNO") to GaOwnMetalRate(23_500, 38_000, 58_000),
        setOf("CGK", "LBJ") to GaOwnMetalRate(21_600, 35_200, 58_000),
        setOf("CGK", "UPG") to GaOwnMetalRate(23_500, 38_000, 58_000),
        setOf("CGK", "PLW") to GaOwnMetalRate(21_600, 35_200, 58_000),
        setOf("CGK", "BTJ") to GaOwnMetalRate(28_800, 42_500, 78_000),
        setOf("CGK", "KOE") to GaOwnMetalRate(28_800, 42_500, 78_000),
        setOf("CGK", "MDC") to GaOwnMetalRate(28_800, 47_300, 78_000),
        setOf("CGK", "KDI") to GaOwnMetalRate(28_800, 42_500, 78_000),
        setOf("CGK", "AMQ") to GaOwnMetalRate(28_800, 47_300, 78_000),
        setOf("CGK", "GTO") to GaOwnMetalRate(28_800, 42_500, 78_000),
        setOf("CGK", "TTE") to GaOwnMetalRate(33_600, 49_500, 90_700),
        setOf("CGK", "SOQ") to GaOwnMetalRate(33_600, 55_000, 90_700),
        setOf("CGK", "DJJ") to GaOwnMetalRate(40_800, 67_100, 110_700),
        setOf("CGK", "TIM") to GaOwnMetalRate(40_800, 67_100, 110_700),
        setOf("CGK", "MKQ") to GaOwnMetalRate(40_800, 67_100, 110_700),
        // Halim (HLP) equivalents
        setOf("HLP", "KNO") to GaOwnMetalRate(23_500, 38_000, 58_000),
        setOf("HLP", "SUB") to GaOwnMetalRate(11_500, 19_000, 25_400),
        setOf("HLP", "DPS") to GaOwnMetalRate(17_900, 32_000, 45_300),
        // Non-Jakarta Domestic
        setOf("BPN", "DPS") to GaOwnMetalRate(16_800, 27_500, 45_300),
        setOf("BPN", "UPG") to GaOwnMetalRate(9_600, 15_400, 25_400),
        setOf("DPS", "LBJ") to GaOwnMetalRate(9_600, 15_400, 25_400),
        setOf("DPS", "UPG") to GaOwnMetalRate(9_600, 13_800, 25_400),
        setOf("DPS", "SOQ") to GaOwnMetalRate(28_800, 47_300, 78_000),
        setOf("DPS", "YIA") to GaOwnMetalRate(9_600, 15_400, 25_400),
        setOf("SUB", "DPS") to GaOwnMetalRate(4_800, 7_700, 12_700),
        setOf("SUB", "KOE") to GaOwnMetalRate(21_600, 31_500, 58_000),
        setOf("UPG", "KDI") to GaOwnMetalRate(4_800, 7_700, 12_700),
        setOf("UPG", "PLW") to GaOwnMetalRate(9_600, 15_400, 25_400),
        setOf("UPG", "GTO") to GaOwnMetalRate(9_600, 15_400, 25_400),
        setOf("UPG", "MDC") to GaOwnMetalRate(16_800, 27_500, 45_300),
        setOf("UPG", "SOQ") to GaOwnMetalRate(21_600, 35_200, 58_000),
        setOf("DJJ", "TIM") to GaOwnMetalRate(9_600, 15_400, 25_400),
        setOf("DJJ", "MKQ") to GaOwnMetalRate(9_600, 15_400, 25_400),
        setOf("DJJ", "BIK") to GaOwnMetalRate(8_600, 15_400, 25_400),
        setOf("DJJ", "SOQ") to GaOwnMetalRate(16_800, 27_500, 45_300)
    )

    private val gaIntlRouteRates: Map<Set<String>, GaOwnMetalRate> = mapOf(
        setOf("CGK", "SIN") to GaOwnMetalRate(14_500, 28_000, 43_200),
        setOf("CGK", "KUL") to GaOwnMetalRate(12_500, 22_500, 43_200),
        setOf("CGK", "BKK") to GaOwnMetalRate(18_500, 33_500, 49_500),
        setOf("CGK", "HKG") to GaOwnMetalRate(25_000, 45_000, 77_700),
        setOf("CGK", "CAN") to GaOwnMetalRate(25_000, 45_000, 77_700),
        setOf("CGK", "PVG") to GaOwnMetalRate(30_000, 54_000, 90_000),
        setOf("CGK", "MEL") to GaOwnMetalRate(42_000, 74_000, 108_000),
        setOf("CGK", "ICN") to GaOwnMetalRate(35_000, 63_000, 108_000),
        setOf("CGK", "SYD") to GaOwnMetalRate(42_000, 74_000, 108_000),
        setOf("CGK", "HND") to GaOwnMetalRate(35_000, 63_000, 108_000),
        setOf("CGK", "NRT") to GaOwnMetalRate(35_000, 63_000, 108_000),
        setOf("CGK", "JED") to GaOwnMetalRate(49_000, 89_000, 126_000),
        setOf("CGK", "MED") to GaOwnMetalRate(49_000, 89_000, 126_000),
        setOf("CGK", "DOH") to GaOwnMetalRate(49_000, 89_000, 126_000),
        setOf("CGK", "AMS") to GaOwnMetalRate(50_000, 110_000, 190_000),
        setOf("DPS", "SIN") to GaOwnMetalRate(15_000, 27_000, 49_500),
        setOf("DPS", "SYD") to GaOwnMetalRate(30_000, 54_000, 90_000),
        setOf("DPS", "ICN") to GaOwnMetalRate(35_000, 63_000, 108_000),
        setOf("DPS", "MEL") to GaOwnMetalRate(30_000, 54_000, 90_000),
        setOf("DPS", "NRT") to GaOwnMetalRate(35_000, 63_000, 108_000),
        setOf("DPS", "HND") to GaOwnMetalRate(35_000, 63_000, 108_000),
        setOf("SUB", "SIN") to GaOwnMetalRate(12_500, 22_500, 43_200)
    )

    private fun calculateGarudaMiles(
        airlineCode: String,
        origin: Airport,
        dest: Airport,
        distanceMi: Int,
        cabin: CabinClass
    ): MilesEstimate {
        val effectiveCabin = when (cabin) {
            CabinClass.SUITES -> CabinClass.FIRST
            CabinClass.ARIA_SUITE -> CabinClass.BUSINESS
            else -> cabin
        }
        val isOwnMetal = airlineCode == "GA"
        val isDomesticIndonesia = origin.country == "Indonesia" && dest.country == "Indonesia"
        val pair = setOf(origin.iata.uppercase(), dest.iata.uppercase())

        if (isOwnMetal && isDomesticIndonesia) {
            val rate = gaDomesticRouteRates[pair] ?: when {
                distanceMi <= 220 -> GaOwnMetalRate(4_800, 7_700, 12_700)
                distanceMi <= 480 -> GaOwnMetalRate(9_600, 15_400, 25_400)
                distanceMi <= 720 -> GaOwnMetalRate(16_800, 27_500, 45_300)
                distanceMi <= 950 -> GaOwnMetalRate(21_600, 35_200, 58_000)
                distanceMi <= 1500 -> GaOwnMetalRate(28_800, 47_300, 78_000)
                else -> GaOwnMetalRate(33_600, 55_000, 90_700)
            }
            val miles = when (effectiveCabin) {
                CabinClass.ECONOMY -> rate.y
                CabinClass.PREMIUM_ECONOMY -> roundTo500((rate.y + rate.j) * 0.48)
                CabinClass.BUSINESS -> rate.j
                CabinClass.FIRST -> rate.f
                CabinClass.SUITES, CabinClass.ARIA_SUITE -> rate.f
            }
            return MilesEstimate(
                miles = miles,
                tierLabel = "GA Domestik (Apr 2026)",
                ruleSummary = "GarudaMiles Own-Metal Domestik ($distanceMi mi)"
            )
        }

        if (isOwnMetal) {
            val rate = gaIntlRouteRates[pair] ?: when {
                distanceMi <= 750 -> GaOwnMetalRate(12_500, 22_500, 43_200)
                distanceMi <= 1500 -> GaOwnMetalRate(18_500, 33_500, 49_500)
                distanceMi <= 2200 -> GaOwnMetalRate(25_000, 45_000, 77_700)
                distanceMi <= 2900 -> GaOwnMetalRate(30_000, 54_000, 90_000)
                distanceMi <= 3700 -> GaOwnMetalRate(35_000, 63_000, 108_000)
                distanceMi <= 5800 -> GaOwnMetalRate(49_000, 89_000, 126_000)
                else -> GaOwnMetalRate(50_000, 110_000, 190_000)
            }
            val miles = when (effectiveCabin) {
                CabinClass.ECONOMY -> rate.y
                CabinClass.PREMIUM_ECONOMY -> roundTo500((rate.y + rate.j) * 0.48)
                CabinClass.BUSINESS -> rate.j
                CabinClass.FIRST -> rate.f
                CabinClass.SUITES, CabinClass.ARIA_SUITE -> rate.f
            }
            return MilesEstimate(
                miles = miles,
                tierLabel = "GA Intl (Apr 2026)",
                ruleSummary = "GarudaMiles Own-Metal Internasional ($distanceMi mi)"
            )
        }

        // SkyTeam & Partner Airline Award Chart (Group A..I, 1-way; Group D Biz = 47,500)
        val band = skyTeamPartnerGroupBand(distanceMi)
        val miles = when (effectiveCabin) {
            CabinClass.ECONOMY -> band.y
            CabinClass.PREMIUM_ECONOMY -> band.w
            CabinClass.BUSINESS -> band.j
            CabinClass.FIRST -> band.f
            CabinClass.SUITES, CabinClass.ARIA_SUITE -> band.f
        }
        val label = if (airlineCode == "EY") "Mitra Etihad" else "SkyTeam / Partner"
        return MilesEstimate(
            miles = miles,
            tierLabel = label,
            ruleSummary = "GarudaMiles $label ${band.label} ($distanceMi mi)"
        )
    }

    private data class DistanceBand(val label: String, val y: Int, val w: Int, val j: Int, val f: Int)

    private fun skyTeamPartnerGroupBand(dist: Int): DistanceBand = when {
        dist <= 1000 -> DistanceBand("Group A (1-1.000 mi)", 15_000, 20_000, 25_000, 37_500)
        dist <= 1500 -> DistanceBand("Group B (1.001-1.500 mi)", 17_500, 23_500, 30_000, 45_000)
        dist <= 2000 -> DistanceBand("Group C (1.501-2.000 mi)", 22_500, 30_000, 37_500, 56_500)
        dist <= 2500 -> DistanceBand("Group D (2.001-2.500 mi)", 27_500, 37_000, 47_500, 71_500)
        dist <= 3000 -> DistanceBand("Group E (2.501-3.000 mi)", 32_500, 45_000, 57_500, 86_500)
        dist <= 4000 -> DistanceBand("Group F (3.001-4.000 mi)", 37_500, 52_500, 67_500, 101_500)
        dist <= 6000 -> DistanceBand("Group G (4.001-6.000 mi)", 42_500, 58_500, 75_000, 112_500)
        dist <= 8000 -> DistanceBand("Group H (6.001-8.000 mi)", 52_500, 73_500, 95_000, 142_500)
        else -> DistanceBand("Group I (8.001-10.000+ mi)", 57_500, 78_500, 100_000, 150_000)
    }

    // =========================================================================
    // 2. KRISFLYER (SQ Zone-to-Zone Saver & Advantage Chart + Star Alliance)
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
            val baseY = max(4_500, roundTo500(distanceMi * 5.0))
            val miles = if (cabin == CabinClass.PREMIUM_ECONOMY) roundTo500(baseY * 1.65) else baseY
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
            val quad = if (useAdvantage) {
                sqAdvantageRates(z1, z2, distanceMi)
            } else {
                sqSaverRates(z1, z2, distanceMi)
            }
            val finalMiles = when (cabin) {
                CabinClass.ECONOMY -> quad.y
                CabinClass.PREMIUM_ECONOMY -> quad.w
                CabinClass.BUSINESS, CabinClass.ARIA_SUITE -> quad.j
                CabinClass.FIRST, CabinClass.SUITES -> quad.f
            }
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
                CabinClass.BUSINESS, CabinClass.ARIA_SUITE -> j
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

    // Official Singapore Airlines KrisFlyer One-Way Saver Awards Chart (in miles)
    private fun sqSaverRates(z1: Int, z2: Int, dist: Int): Quad {
        // Explicit SQ Fifth Freedom / special sub-zone routes
        if (z1 == 11 && z2 == 11) return Quad(11_500, 20_000, 36_000, 50_500) // MXP-BCN
        if (z1 == 11 && z2 == 13) return Quad(27_500, 57_000, 89_000, 106_500) // FRA-JFK / MAN-IAH
        if (z1 == 7 && z2 == 12) return Quad(42_500, 80_500, 114_000, 132_500) // NRT-LAX

        val table: Map<Pair<Int, Int>, Quad> = mapOf(
            // Zone 1 (Singapore)
            (1 to 1) to Quad(8_000, 14_000, 22_000, 32_000),
            (1 to 2) to Quad(8_000, 14_000, 22_000, 32_000),
            (1 to 3) to Quad(13_000, 20_000, 25_000, 38_000),
            (1 to 4) to Quad(15_500, 28_000, 35_500, 47_500),
            (1 to 5) to Quad(20_500, 36_000, 45_000, 61_500),
            (1 to 6) to Quad(19_000, 36_000, 45_000, 61_500),
            (1 to 7) to Quad(25_500, 39_500, 54_500, 81_000),
            (1 to 8) to Quad(20_500, 34_000, 42_500, 60_500),
            (1 to 9) to Quad(29_000, 53_500, 72_000, 98_000),
            (1 to 10) to Quad(32_000, 51_500, 68_000, 95_000),
            (1 to 11) to Quad(44_000, 74_500, 108_500, 148_000),
            (1 to 12) to Quad(44_000, 79_000, 112_500, 154_000),
            (1 to 13) to Quad(46_000, 84_500, 117_000, 156_000),
            // Zone 2 (Malaysia, Indonesia, Brunei)
            (2 to 2) to Quad(8_500, 15_000, 23_000, 33_500),
            (2 to 3) to Quad(13_500, 20_500, 26_500, 39_500),
            (2 to 4) to Quad(16_000, 28_500, 36_500, 48_500),
            (2 to 5) to Quad(21_000, 37_500, 46_500, 63_000),
            (2 to 6) to Quad(20_000, 38_000, 47_500, 64_500),
            (2 to 7) to Quad(27_000, 41_500, 57_000, 84_500),
            (2 to 8) to Quad(21_500, 35_000, 44_500, 63_500),
            (2 to 9) to Quad(30_500, 56_000, 75_500, 103_000),
            (2 to 10) to Quad(33_500, 54_000, 70_500, 99_500),
            (2 to 11) to Quad(46_000, 78_000, 114_000, 155_000),
            (2 to 12) to Quad(46_000, 82_500, 117_500, 161_000),
            (2 to 13) to Quad(48_500, 88_500, 122_500, 163_500),
            // Zone 3 (Philippines, Thailand, Vietnam, Myanmar, Cambodia, Laos)
            (3 to 3) to Quad(13_500, 20_500, 26_500, 39_500),
            (3 to 6) to Quad(20_000, 38_000, 47_500, 64_500),
            (3 to 7) to Quad(27_000, 41_500, 57_000, 84_500),
            (3 to 8) to Quad(21_500, 35_000, 44_500, 63_500),
            (3 to 9) to Quad(30_500, 56_000, 75_500, 103_000),
            (3 to 10) to Quad(33_500, 54_000, 70_500, 99_500),
            (3 to 11) to Quad(46_000, 78_000, 114_000, 155_000),
            (3 to 12) to Quad(46_000, 82_500, 117_500, 161_000),
            (3 to 13) to Quad(48_500, 88_500, 122_500, 163_500),
            // Zone 4 (Mainland China excl. PEK/PVG, HKG, Taiwan)
            (4 to 6) to Quad(25_000, 50_000, 62_000, 81_500),
            (4 to 8) to Quad(21_500, 35_000, 44_500, 66_500),
            (4 to 9) to Quad(30_000, 57_000, 77_000, 105_000),
            (4 to 10) to Quad(46_500, 78_000, 100_000, 139_500),
            (4 to 11) to Quad(51_500, 97_000, 128_000, 174_000),
            (4 to 13) to Quad(57_000, 107_000, 134_000, 182_500),
            // Zone 5 (Beijing & Shanghai)
            (5 to 6) to Quad(36_000, 60_500, 78_500, 93_500),
            (5 to 8) to Quad(32_000, 52_000, 68_000, 86_500),
            (5 to 9) to Quad(40_000, 80_500, 100_000, 126_500),
            (5 to 10) to Quad(46_500, 85_000, 102_000, 139_500),
            (5 to 11) to Quad(51_000, 103_000, 123_000, 168_000),
            // Zone 6 (South Asia)
            (6 to 7) to Quad(42_000, 64_500, 88_000, 114_500),
            (6 to 8) to Quad(32_000, 55_000, 75_000, 113_000),
            (6 to 9) to Quad(42_000, 79_000, 100_000, 138_000),
            (6 to 12) to Quad(55_000, 105_500, 133_000, 186_500),
            (6 to 13) to Quad(65_500, 111_000, 147_500, 194_500),
            // Zone 7 (Japan & South Korea)
            (7 to 8) to Quad(37_000, 58_000, 79_000, 100_500),
            (7 to 9) to Quad(44_500, 83_000, 108_500, 136_500),
            (7 to 10) to Quad(58_500, 89_000, 112_500, 145_500),
            (7 to 11) to Quad(55_000, 106_000, 135_000, 186_500),
            // Zone 8 (Perth & Darwin)
            (8 to 10) to Quad(46_500, 85_000, 102_000, 145_000),
            (8 to 11) to Quad(52_500, 84_500, 131_500, 176_500),
            (8 to 12) to Quad(61_000, 107_000, 137_500, 194_500),
            (8 to 13) to Quad(66_500, 115_000, 150_000, 198_000),
            // Zone 9 (Australia excl. PER/DRW & New Zealand)
            (9 to 10) to Quad(62_500, 92_500, 124_000, 161_500),
            (9 to 11) to Quad(63_000, 109_000, 141_000, 198_000),
            (9 to 13) to Quad(71_500, 117_500, 157_000, 203_000),
            // Zone 10 (Africa, Middle East & Turkey)
            (10 to 12) to Quad(69_000, 137_000, 162_500, 209_500)
        )
        return table[z1 to z2] ?: when {
            dist <= 1000 -> Quad(13_500, 20_500, 26_500, 39_500)
            dist <= 2500 -> Quad(20_000, 36_000, 45_000, 61_500)
            dist <= 4500 -> Quad(30_500, 56_000, 75_500, 103_000)
            dist <= 7000 -> Quad(46_000, 78_000, 114_000, 155_000)
            else -> Quad(48_500, 88_500, 122_500, 163_500)
        }
    }

    // Official Singapore Airlines KrisFlyer One-Way Advantage Awards Chart (in miles)
    // Note: Per SQ chart notes, Premium Economy tickets refer to the Saver Awards chart.
    private fun sqAdvantageRates(z1: Int, z2: Int, dist: Int): Quad {
        val saver = sqSaverRates(z1, z2, dist)
        if (z1 == 11 && z2 == 11) return Quad(41_500, saver.w, 85_500, 171_000) // MXP-BCN
        if (z1 == 11 && z2 == 13) return Quad(47_000, saver.w, 105_000, 172_000) // FRA-JFK / MAN-IAH
        if (z1 == 7 && z2 == 12) return Quad(76_500, saver.w, 147_500, 224_500) // NRT-LAX

        val table: Map<Pair<Int, Int>, Quad> = mapOf(
            // Zone 1 (Singapore)
            (1 to 1) to Quad(16_500, saver.w, 40_500, 60_500),
            (1 to 2) to Quad(16_500, saver.w, 40_500, 60_500),
            (1 to 3) to Quad(27_500, saver.w, 46_000, 72_000),
            (1 to 4) to Quad(33_000, saver.w, 57_500, 84_000),
            (1 to 5) to Quad(44_000, saver.w, 75_000, 112_500),
            (1 to 6) to Quad(40_500, saver.w, 75_000, 112_500),
            (1 to 7) to Quad(49_500, saver.w, 80_500, 138_000),
            (1 to 8) to Quad(40_500, saver.w, 75_000, 114_500),
            (1 to 9) to Quad(60_500, saver.w, 103_500, 178_500),
            (1 to 10) to Quad(66_500, saver.w, 122_000, 171_000),
            (1 to 11) to Quad(79_000, saver.w, 141_500, 259_500),
            (1 to 12) to Quad(79_000, saver.w, 148_000, 262_000),
            (1 to 13) to Quad(84_500, saver.w, 165_000, 279_500),
            // Zone 2 (Malaysia, Indonesia, Brunei)
            (2 to 2) to Quad(17_500, saver.w, 42_000, 63_000),
            (2 to 3) to Quad(28_500, saver.w, 48_000, 75_000),
            (2 to 4) to Quad(34_000, saver.w, 59_000, 86_000),
            (2 to 5) to Quad(45_000, saver.w, 76_500, 115_500),
            (2 to 6) to Quad(42_500, saver.w, 78_000, 117_500),
            (2 to 7) to Quad(51_500, saver.w, 84_000, 144_000),
            (2 to 8) to Quad(42_500, saver.w, 78_000, 119_500),
            (2 to 9) to Quad(63_000, saver.w, 108_000, 186_000),
            (2 to 10) to Quad(70_000, saver.w, 127_500, 178_000),
            (2 to 11) to Quad(82_500, saver.w, 147_500, 270_500),
            (2 to 12) to Quad(82_500, saver.w, 154_000, 273_500),
            (2 to 13) to Quad(88_000, saver.w, 172_000, 291_500),
            // Zone 3 (Philippines, Thailand, Vietnam, Myanmar, Cambodia, Laos)
            (3 to 3) to Quad(28_500, saver.w, 48_000, 75_000),
            (3 to 6) to Quad(42_500, saver.w, 78_000, 117_500),
            (3 to 7) to Quad(51_500, saver.w, 84_000, 144_000),
            (3 to 8) to Quad(42_500, saver.w, 78_000, 119_500),
            (3 to 9) to Quad(63_000, saver.w, 108_000, 186_000),
            (3 to 10) to Quad(70_000, saver.w, 127_500, 178_000),
            (3 to 11) to Quad(82_500, saver.w, 147_500, 270_500),
            (3 to 12) to Quad(82_500, saver.w, 154_000, 273_500),
            (3 to 13) to Quad(88_000, saver.w, 172_000, 291_500),
            // Zone 4 (Mainland China excl. PEK/PVG, HKG, Taiwan)
            (4 to 6) to Quad(53_000, saver.w, 100_500, 148_500),
            (4 to 8) to Quad(42_500, saver.w, 78_000, 123_000),
            (4 to 9) to Quad(62_000, saver.w, 106_000, 186_500),
            (4 to 10) to Quad(95_000, saver.w, 177_000, 255_000),
            (4 to 11) to Quad(92_500, saver.w, 169_500, 306_000),
            (4 to 13) to Quad(104_000, saver.w, 187_500, 321_000),
            // Zone 5 (Beijing & Shanghai)
            (5 to 6) to Quad(75_500, saver.w, 130_000, 172_500),
            (5 to 8) to Quad(64_500, saver.w, 118_000, 162_500),
            (5 to 9) to Quad(84_500, saver.w, 147_500, 227_500),
            (5 to 10) to Quad(95_000, saver.w, 177_000, 255_000),
            (5 to 11) to Quad(98_000, saver.w, 176_000, 276_000),
            // Zone 6 (South Asia)
            (6 to 7) to Quad(84_500, saver.w, 135_500, 198_000),
            (6 to 8) to Quad(64_500, saver.w, 130_000, 215_500),
            (6 to 9) to Quad(84_500, saver.w, 147_500, 251_500),
            (6 to 12) to Quad(98_000, saver.w, 176_000, 314_500),
            (6 to 13) to Quad(121_500, saver.w, 212_000, 345_000),
            // Zone 7 (Japan & South Korea)
            (7 to 8) to Quad(70_000, saver.w, 124_000, 176_500),
            (7 to 9) to Quad(90_500, saver.w, 159_500, 236_000),
            (7 to 10) to Quad(108_000, saver.w, 184_000, 244_000),
            (7 to 11) to Quad(98_000, saver.w, 176_000, 321_000),
            // Zone 8 (Perth & Darwin)
            (8 to 10) to Quad(95_000, saver.w, 177_000, 263_500),
            (8 to 11) to Quad(94_000, saver.w, 160_000, 294_500),
            (8 to 12) to Quad(109_500, saver.w, 187_500, 338_500),
            (8 to 13) to Quad(123_500, saver.w, 215_500, 351_000),
            // Zone 9 (Australia excl. PER/DRW & New Zealand)
            (9 to 10) to Quad(114_000, saver.w, 205_500, 277_000),
            (9 to 11) to Quad(109_500, saver.w, 181_500, 333_000),
            (9 to 13) to Quad(127_000, saver.w, 224_000, 357_000),
            // Zone 10 (Africa, Middle East & Turkey)
            (10 to 12) to Quad(120_500, saver.w, 219_500, 356_500)
        )
        return table[z1 to z2] ?: Quad(
            roundTo500(saver.y * 1.80),
            saver.w,
            roundTo500(saver.j * 1.45),
            roundTo500(saver.f * 1.75)
        )
    }

    private fun starPartnerRates(z1: Int, z2: Int, dist: Int): Quad {
        if (z1 == z2) {
            return when (z1) {
                1, 2, 3 -> Quad(13_500, 20_000, 25_500, 38_500)
                4, 5, 7 -> Quad(13_500, 21_500, 29_000, 43_000)
                11 -> Quad(13_500, 21_000, 29_000, 45_000)
                12, 13 -> Quad(15_000, 22_000, 29_000, 40_000)
                else -> Quad(15_000, 23_000, 31_000, 46_000)
            }
        }
        if (z1 in 1..3 && z2 in 1..3) return Quad(13_500, 20_000, 25_500, 38_500)
        if (z1 in 1..3 && z2 in 4..5) return Quad(22_500, 35_000, 46_000, 64_000)
        if (z1 in 1..3 && z2 == 6) return Quad(23_500, 36_000, 49_000, 66_000)
        if (z1 in 1..3 && z2 == 7) return Quad(28_500, 44_000, 56_000, 82_000)
        if (z1 in 1..3 && z2 in 8..9) return Quad(32_000, 54_000, 72_500, 99_000)
        if (z1 in 1..3 && z2 == 10) return Quad(31_500, 50_000, 63_000, 92_000)
        if (z1 in 1..3 && z2 == 11) return Quad(46_000, 76_000, 108_500, 152_000)
        if (z1 in 1..3 && z2 in 12..13) return Quad(51_000, 84_000, 121_500, 165_000)
        if (z1 in 4..7 && z2 == 11) return Quad(44_500, 74_000, 105_000, 146_000)
        if (z1 in 4..7 && z2 in 12..13) return Quad(47_000, 79_000, 114_000, 154_000)
        if (z1 == 11 && z2 in 12..13) return Quad(30_500, 56_000, 81_000, 115_000)
        if (z1 == 10 && z2 == 11) return Quad(26_000, 42_000, 55_000, 82_000)
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
    // 3. ASIA MILES (Cathay Pacific Standard Award Chart + oneworld Partner)
    // =========================================================================
    private fun calculateAsiaMiles(
        airlineCode: String,
        origin: Airport,
        dest: Airport,
        distanceMi: Int,
        cabin: CabinClass,
        isMultiCarrier: Boolean
    ): MilesEstimate {
        val effectiveCabin = when (cabin) {
            CabinClass.SUITES -> CabinClass.FIRST
            CabinClass.ARIA_SUITE -> CabinClass.BUSINESS
            else -> cabin
        }

        if (isMultiCarrier) {
            val band = oneworldMultiCarrierBand(distanceMi)
            val miles = when (effectiveCabin) {
                CabinClass.ECONOMY -> band.y
                CabinClass.PREMIUM_ECONOMY -> band.w
                CabinClass.BUSINESS -> band.j
                CabinClass.FIRST -> band.f
                CabinClass.SUITES, CabinClass.ARIA_SUITE -> band.f
            }
            return MilesEstimate(
                miles = miles,
                tierLabel = "oneworld Multi-Carrier",
                ruleSummary = "Asia Miles Multi-Carrier ${band.label} ($distanceMi mi)"
            )
        }

        val isCX = airlineCode == "CX"
        val band = if (isCX) {
            cxDistanceBand(origin, dest, distanceMi)
        } else {
            oneworldPartnerBand(distanceMi)
        }
        val miles = when (effectiveCabin) {
            CabinClass.ECONOMY -> band.y
            CabinClass.PREMIUM_ECONOMY -> band.w
            CabinClass.BUSINESS -> band.j
            CabinClass.FIRST -> band.f
            CabinClass.SUITES, CabinClass.ARIA_SUITE -> band.f
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

    private fun isType2ShortHaul(origin: Airport, dest: Airport): Boolean {
        val type2Countries = setOf("Indonesia", "Japan", "India", "Bangladesh", "Nepal", "Sri Lanka")
        return origin.country in type2Countries || dest.country in type2Countries
    }

    // Cathay Pacific Asia Miles Standard Award Chart (1-Way, matching AM Calculator reference):
    // Ultra-Short (1-750 mi): 7.000 Eco | 15.000 P.Eco | 16.000 Biz/Aria | 27.000 First/Suite
    // Short-Haul Type 1 (751-2.750 mi): 9.000 Eco | 18.000 P.Eco | 28.000 Biz/Aria | 43.000 First/Suite
    // Short-Haul Type 2 (751-2.750 mi ID/JP/South Asia): 13.000 Eco | 23.000 P.Eco | 33.000 Biz/Aria | 50.000 First/Suite
    // Medium-Haul (2.751-5.000 mi): 20.000 Eco | 40.000 P.Eco | 58.000 Biz/Aria | 85.000 First/Suite
    // Long-Haul (5.001-7.500 mi): 27.000 Eco | 50.000 P.Eco | 88.000 Biz/Aria | 125.000 First/Suite
    // Ultra-Long (7.501+ mi): 38.000 Eco | 75.000 P.Eco | 115.000 Biz/Aria | 160.000 First/Suite
    private fun cxDistanceBand(origin: Airport, dest: Airport, dist: Int): DistanceBand = when {
        dist <= 750 -> DistanceBand("Ultra-Short ≤750 mi", 7_000, 15_000, 16_000, 27_000)
        dist <= 2750 && isType2ShortHaul(origin, dest) ->
            DistanceBand("Short-Haul 751-2.750 mi (Type 2)", 13_000, 23_000, 33_000, 50_000)
        dist <= 2750 ->
            DistanceBand("Short-Haul 751-2.750 mi (Type 1)", 9_000, 18_000, 28_000, 43_000)
        dist <= 5000 -> DistanceBand("Medium-Haul 2.751-5.000 mi", 20_000, 40_000, 58_000, 85_000)
        dist <= 7500 -> DistanceBand("Long-Haul 5.001-7.500 mi", 27_000, 50_000, 88_000, 125_000)
        else -> DistanceBand("Ultra-Long 7.501+ mi", 38_000, 75_000, 115_000, 160_000)
    }

    private fun oneworldPartnerBand(dist: Int): DistanceBand = when {
        dist <= 750 -> DistanceBand("≤750 mi", 9_000, 17_000, 19_000, 32_000)
        dist <= 2750 -> DistanceBand("751-2.750 mi", 14_000, 25_000, 34_000, 52_000)
        dist <= 5000 -> DistanceBand("2.751-5.000 mi", 27_000, 43_000, 63_000, 100_000)
        dist <= 7500 -> DistanceBand("5.001-7.500 mi", 40_000, 58_000, 93_000, 135_000)
        else -> DistanceBand("7.501+ mi", 47_000, 80_000, 120_000, 170_000)
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

    private fun roundTo500(value: Double): Int {
        return roundTo500(value.roundToInt())
    }
}
