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
                CabinClass.BUSINESS, CabinClass.ARIA_SUITE -> rate.j
                CabinClass.FIRST, CabinClass.SUITES -> rate.f
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
                CabinClass.BUSINESS, CabinClass.ARIA_SUITE -> rate.j
                CabinClass.FIRST, CabinClass.SUITES -> rate.f
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
            CabinClass.BUSINESS, CabinClass.ARIA_SUITE -> band.j
            CabinClass.FIRST, CabinClass.SUITES -> band.f
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
    // 2. KRISFLYER (SQ Zone Chart Saver/Advantage 2026 + Star Alliance Chart)
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
            // Scoot KrisFlyer distance-equivalent redemption (2026)
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
            val (y, w, j, f) = sqSaverRates(z1, z2, distanceMi)
            val base = when (cabin) {
                CabinClass.ECONOMY -> y
                CabinClass.PREMIUM_ECONOMY -> w
                CabinClass.BUSINESS, CabinClass.ARIA_SUITE -> j
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

    private fun sqAdvantageMultiplier(saver: Int, cabin: CabinClass): Int {
        val factor = when (cabin) {
            CabinClass.ECONOMY -> 1.80
            CabinClass.PREMIUM_ECONOMY -> 1.50
            CabinClass.BUSINESS, CabinClass.ARIA_SUITE -> 1.62
            CabinClass.FIRST, CabinClass.SUITES -> 1.75
        }
        return roundTo500(saver * factor)
    }

    // Updated Singapore Airlines KrisFlyer Saver 1-way table (Zone 1..13, 2026)
    // e.g. SIN<->Z2 (ID/MY/BN): 8k Y / 22k J; SIN<->Z3 (SE Asia): 13k Y / 25k J;
    // SIN<->Z7 (JP/KR): 25.5k Y / 39.5k W / 54.5k J / 81k F;
    // SIN<->Z8 (PER/DRW): 20.5k Y / 42.5k J / 60.5k F;
    // SIN<->Z9 (SYD/MEL/AKL): 29k Y / 72k J / 98k F;
    // SIN<->Z11 (Europe): 44k Y / 74.5k W / 108.5k J / 148k F;
    // SIN<->Z12 (US West): 44k Y / 79k W / 112.5k J / 154k F;
    // SIN<->Z13 (US East): 46k Y / 84.5k W / 117k J / 156k F
    private fun sqSaverRates(z1: Int, z2: Int, dist: Int): Quad {
        if (z1 == 1) { // From/To Singapore (Zone 1)
            return when (z2) {
                1, 2 -> Quad(8_000, 14_000, 22_000, 31_500)      // SIN <-> ID/MY/BN
                3 -> Quad(13_000, 20_000, 25_000, 34_500)        // SIN <-> TH/VN/PH/KH/LA/MM
                4 -> Quad(16_000, 26_000, 35_000, 46_500)        // SIN <-> HKG/TPE/South China
                5 -> Quad(21_000, 34_000, 44_500, 60_500)        // SIN <-> PVG/PEK
                6 -> Quad(19_500, 34_000, 44_500, 60_500)        // SIN <-> South Asia
                7 -> Quad(25_500, 39_500, 54_500, 81_000)        // SIN <-> Japan / South Korea
                8 -> Quad(20_500, 34_000, 42_500, 60_500)        // SIN <-> Perth / Darwin
                9 -> Quad(29_000, 52_000, 72_000, 98_000)        // SIN <-> SYD / MEL / AKL
                10 -> Quad(28_500, 46_000, 58_500, 88_000)       // SIN <-> Africa / Middle East / Turkey
                11 -> Quad(44_000, 74_500, 108_500, 148_000)     // SIN <-> Europe
                12 -> Quad(44_000, 79_000, 112_500, 154_000)     // SIN <-> US West Coast
                else -> Quad(46_000, 84_500, 117_000, 156_000)   // SIN <-> US East Coast & Houston
            }
        }
        if (z1 == 2) { // From/To Indonesia / Malaysia / Brunei (Zone 2) via SIN
            return when (z2) {
                2 -> Quad(10_000, 16_500, 24_000, 34_500)
                3 -> Quad(13_000, 20_000, 25_000, 34_500)
                4 -> Quad(16_000, 26_000, 35_000, 46_500)
                5 -> Quad(21_000, 34_000, 44_500, 60_500)
                6 -> Quad(19_500, 34_000, 44_500, 60_500)
                7 -> Quad(25_500, 39_500, 54_500, 81_000)
                8 -> Quad(20_500, 34_000, 42_500, 60_500)
                9 -> Quad(29_000, 52_000, 72_000, 98_000)
                10 -> Quad(28_500, 46_000, 58_500, 88_000)
                11 -> Quad(44_000, 74_500, 108_500, 148_000)
                12 -> Quad(44_000, 79_000, 112_500, 154_000)
                else -> Quad(46_000, 84_500, 117_000, 156_000)
            }
        }
        // SQ Fifth Freedom & Inter-zone routes
        if (z1 == 11 && z2 == 13) return Quad(26_000, 54_000, 84_000, 101_000)
        if (z1 == 7 && z2 == 12) return Quad(39_500, 75_000, 106_500, 124_000)
        if (z1 == 11 && z2 == 11) return Quad(13_000, 21_000, 26_500, 39_500)

        return when {
            dist <= 1000 -> Quad(13_000, 20_500, 26_000, 39_500)
            dist <= 2500 -> Quad(19_500, 34_000, 44_500, 60_500)
            dist <= 4500 -> Quad(29_000, 52_000, 72_000, 98_000)
            dist <= 7000 -> Quad(44_000, 74_500, 108_500, 148_000)
            else -> Quad(46_000, 84_500, 117_000, 156_000)
        }
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
    // 3. ASIA MILES (Cathay Pacific Repriced Chart eff. 1 May 2026 + oneworld)
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
                CabinClass.BUSINESS, CabinClass.ARIA_SUITE -> band.j
                CabinClass.FIRST, CabinClass.SUITES -> band.f
            }
            return MilesEstimate(
                miles = miles,
                tierLabel = "oneworld Multi-Carrier",
                ruleSummary = "Asia Miles Multi-Carrier ${band.label} ($distanceMi mi)"
            )
        }

        val isCX = airlineCode == "CX"
        val band = if (isCX) {
            cxDistanceBandMay2026(origin, dest, distanceMi)
        } else {
            oneworldPartnerBandMay2026(distanceMi)
        }
        val miles = when (effectiveCabin) {
            CabinClass.ECONOMY -> band.y
            CabinClass.PREMIUM_ECONOMY -> band.w
            CabinClass.BUSINESS, CabinClass.ARIA_SUITE -> band.j
            CabinClass.FIRST, CabinClass.SUITES -> band.f
        }
        val label = when {
            isCX -> "CX May 2026"
            airlineCode in setOf("LA", "GF", "PG") -> "Mitra Asia Miles"
            else -> "oneworld Partner"
        }
        return MilesEstimate(
            miles = miles,
            tierLabel = label,
            ruleSummary = "Asia Miles $label ${band.label} ($distanceMi mi)"
        )
    }

    private fun isType2MediumHaul(origin: Airport, dest: Airport): Boolean {
        val type2Countries = setOf("Indonesia", "Japan", "India", "Bangladesh", "Nepal", "Sri Lanka")
        return origin.country in type2Countries || dest.country in type2Countries
    }

    // Cathay Pacific Asia Miles Standard Award Chart effective 1 May 2026 (1-way)
    // Ultra-Short <=750 mi: 7k Y / 16k J / 25k F
    // Medium 751-2,750 mi Type 1: 9k Y / 18k W / 27k J / 43k F
    // Medium 751-2,750 mi Type 2 (ID/JP/IN/etc.): 13k Y / 22k W / 32k J / 50k F
    // Long 2,751-5,000 mi: 20k Y / 40k W / 60k J / 90k F
    // Ultra-Long 5,001-7,500 mi: 27k Y / 52k W / 91k J / 125k F
    // Longest 7,501+ mi: 38k Y / 78k W / 119k J / 160k F
    private fun cxDistanceBandMay2026(origin: Airport, dest: Airport, dist: Int): DistanceBand = when {
        dist <= 750 -> DistanceBand("≤750 mi", 7_000, 12_000, 16_000, 25_000)
        dist <= 2750 && isType2MediumHaul(origin, dest) ->
            DistanceBand("751-2.750 mi (Type 2)", 13_000, 22_000, 32_000, 50_000)
        dist <= 2750 ->
            DistanceBand("751-2.750 mi (Type 1)", 9_000, 18_000, 27_000, 43_000)
        dist <= 5000 -> DistanceBand("2.751-5.000 mi", 20_000, 40_000, 60_000, 90_000)
        dist <= 7500 -> DistanceBand("5.001-7.500 mi", 27_000, 52_000, 91_000, 125_000)
        else -> DistanceBand("7.501+ mi", 38_000, 78_000, 119_000, 160_000)
    }

    private fun oneworldPartnerBandMay2026(dist: Int): DistanceBand = when {
        dist <= 750 -> DistanceBand("≤750 mi", 9_000, 14_000, 19_000, 30_000)
        dist <= 2750 -> DistanceBand("751-2.750 mi", 14_000, 24_000, 33_000, 52_000)
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
