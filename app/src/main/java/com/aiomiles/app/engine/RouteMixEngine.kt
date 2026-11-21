package com.aiomiles.app.engine

import com.aiomiles.app.data.CabinClass
import com.aiomiles.app.data.FlightRoute
import com.aiomiles.app.data.ItineraryRecommendation
import com.aiomiles.app.data.ItineraryType
import com.aiomiles.app.data.LegPlan
import com.aiomiles.app.data.LoadedDatabase
import com.aiomiles.app.data.MilesProgram
import com.aiomiles.app.data.MilesWallet
import com.aiomiles.app.data.formatMiles
import kotlin.math.max

data class MixSearchResult(
    val originIata: String,
    val destIata: String,
    val directDistanceMi: Int,
    val cabin: CabinClass,
    val isRoundTrip: Boolean,
    val recommendations: List<ItineraryRecommendation>,
    val bestSingleAlliance: ItineraryRecommendation?,
    val bestCrossAllianceMix: ItineraryRecommendation?
)

object RouteMixEngine {

    fun search(
        db: LoadedDatabase,
        originIata: String,
        destIata: String,
        cabin: CabinClass,
        isRoundTrip: Boolean,
        useKfAdvantage: Boolean,
        wallet: MilesWallet
    ): MixSearchResult {
        val origin = db.airportMap[originIata]
        val dest = db.airportMap[destIata]
        if (origin == null || dest == null || originIata == destIata) {
            return MixSearchResult(originIata, destIata, 0, cabin, isRoundTrip, emptyList(), null, null)
        }

        val mult = if (isRoundTrip) 2 else 1
        val directDist = AwardCalculator.haversineMiles(origin, dest)
        val maxDetourDist = max((directDist * 1.48).toInt(), directDist + 1250)

        val results = mutableListOf<ItineraryRecommendation>()

        // ---------------------------------------------------------------------
        // 1. DIRECT FLIGHTS
        // ---------------------------------------------------------------------
        val outFromOrigin = db.outgoingByAirport[originIata].orEmpty()
        val directRoutes = outFromOrigin.filter { it.dest == destIata }

        for (route in directRoutes) {
            val matchedCabin = resolveCabinForLeg(route, cabin, allowFallback = false) ?: continue
            val est = AwardCalculator.calculateOneWay(
                program = route.program,
                airlineCode = route.airlineCode,
                origin = origin,
                dest = dest,
                distanceMi = route.distanceMi,
                cabin = matchedCabin,
                useKfAdvantage = useKfAdvantage
            )
            val total = est.miles * mult
            val alName = db.airlineMap[route.airlineCode]?.name ?: route.airlineCode
            val badge = when {
                matchedCabin == CabinClass.SUITES -> "A380 Suites (${route.airlineCode})"
                matchedCabin == CabinClass.ARIA_SUITE -> "CX Aria Suite Direct"
                route.isFifthFreedom -> "Fifth Freedom Direct"
                else -> "Nonstop ${route.alliance}"
            }
            val leg = LegPlan(
                route = route,
                airlineName = alName,
                originCity = origin.city,
                destCity = dest.city,
                program = route.program,
                miles = total,
                tierLabel = est.tierLabel,
                cabinUsed = matchedCabin
            )
            results.add(
                ItineraryRecommendation(
                    id = "DIR_${route.origin}_${route.dest}_${route.airlineCode}",
                    type = ItineraryType.DIRECT,
                    legs = listOf(leg),
                    totalMiles = total,
                    totalDistanceMi = route.distanceMi * mult,
                    milesByProgram = mapOf(route.program to total),
                    highlightBadge = badge,
                    explanation = "${est.ruleSummary}. Penerbangan langsung tanpa transit menggunakan ${route.program.displayName}.",
                    transferHub = null
                )
            )
        }

        // ---------------------------------------------------------------------
        // 2. 1-STOP CONNECTIONS (SAME ALLIANCE & CROSS-ALLIANCE MIX)
        // ---------------------------------------------------------------------
        val sameAllianceCandidates = mutableListOf<ItineraryRecommendation>()
        val crossAllianceCandidates = mutableListOf<ItineraryRecommendation>()

        // Group leg1 routes by hub to avoid combinatorial explosion
        val leg1ByHub = outFromOrigin
            .filter { it.dest != destIata }
            .groupBy { it.dest }

        for ((hubIata, leg1List) in leg1ByHub) {
            val hubAirport = db.airportMap[hubIata] ?: continue
            val leg2All = db.outgoingByAirport[hubIata]?.filter { it.dest == destIata }.orEmpty()
            if (leg2All.isEmpty()) continue

            val leg1Dist = leg1List.first().distanceMi
            val leg2Dist = leg2All.first().distanceMi
            val combinedDist = leg1Dist + leg2Dist
            if (combinedDist > maxDetourDist) continue

            // Keep best representative per program for leg1 & leg2
            val bestLeg1ByProgram = pickBestPerProgram(leg1List, cabin, isLongLeg = leg1Dist >= leg2Dist)
            val bestLeg2ByProgram = pickBestPerProgram(leg2All, cabin, isLongLeg = leg2Dist >= leg1Dist)

            for ((prog1, pair1) in bestLeg1ByProgram) {
                val (r1, c1) = pair1
                for ((prog2, pair2) in bestLeg2ByProgram) {
                    val (r2, c2) = pair2
                    // If user strictly chose A380 Suites or CX Aria Suite, at least one leg must feature that cabin!
                    if (cabin == CabinClass.SUITES && c1 != CabinClass.SUITES && c2 != CabinClass.SUITES) {
                        continue
                    }
                    if (cabin == CabinClass.ARIA_SUITE && c1 != CabinClass.ARIA_SUITE && c2 != CabinClass.ARIA_SUITE) {
                        continue
                    }

                    val al1Name = db.airlineMap[r1.airlineCode]?.name ?: r1.airlineCode
                    val al2Name = db.airlineMap[r2.airlineCode]?.name ?: r2.airlineCode

                    val est1 = AwardCalculator.calculateOneWay(
                        program = prog1,
                        airlineCode = r1.airlineCode,
                        origin = origin,
                        dest = hubAirport,
                        distanceMi = r1.distanceMi,
                        cabin = c1,
                        useKfAdvantage = useKfAdvantage
                    )
                    val est2 = AwardCalculator.calculateOneWay(
                        program = prog2,
                        airlineCode = r2.airlineCode,
                        origin = hubAirport,
                        dest = dest,
                        distanceMi = r2.distanceMi,
                        cabin = c2,
                        useKfAdvantage = useKfAdvantage
                    )

                    val m1 = est1.miles * mult
                    val m2 = est2.miles * mult
                    val splitTotal = m1 + m2

                    if (prog1 == prog2) {
                        val legPlan1 = LegPlan(r1, al1Name, origin.city, hubAirport.city, prog1, m1, est1.tierLabel, c1)
                        val legPlan2 = LegPlan(r2, al2Name, hubAirport.city, dest.city, prog2, m2, est2.tierLabel, c2)

                        // KrisFlyer own-metal (Singapore Airlines SQ on both legs):
                        // 1 PNR calculated from total combined distance/zones;
                        // non-SQ KrisFlyer, GarudaMiles, and Asia Miles calculated per leg.
                        val isKfSqOwnMetal1Pnr = prog1 == MilesProgram.KRISFLYER && r1.airlineCode == "SQ" && r2.airlineCode == "SQ"

                        if (isKfSqOwnMetal1Pnr) {
                            val throughCabin = when {
                                c1 == CabinClass.SUITES || c2 == CabinClass.SUITES -> CabinClass.SUITES
                                c1 == CabinClass.ARIA_SUITE || c2 == CabinClass.ARIA_SUITE -> CabinClass.ARIA_SUITE
                                else -> cabin
                            }
                            val throughEst = AwardCalculator.calculateOneWay(
                                program = prog1,
                                airlineCode = "SQ",
                                origin = origin,
                                dest = dest,
                                distanceMi = combinedDist,
                                cabin = throughCabin,
                                useKfAdvantage = useKfAdvantage,
                                isMultiCarrierOneWorld = false
                            )
                            val throughTotal = throughEst.miles * mult
                            val saved = splitTotal - throughTotal
                            val saveText = when {
                                saved > 0 -> "1 PNR SQ hemat ${formatMiles(saved)} mi"
                                saved < 0 -> "1 PNR SQ (+${formatMiles(-saved)} mi vs per leg)"
                                else -> "1 PNR SQ (total jarak)"
                            }
                            val mixCabinNote = if (c1 != c2) " (Mix kabin ${c1.shortLabel}+${c2.shortLabel})" else ""
                            sameAllianceCandidates.add(
                                ItineraryRecommendation(
                                    id = "SAME1_${r1.airlineCode}_${hubIata}_${r2.airlineCode}",
                                    type = ItineraryType.SAME_ALLIANCE_SINGLE,
                                    legs = listOf(legPlan1, legPlan2),
                                    totalMiles = throughTotal,
                                    totalDistanceMi = combinedDist * mult,
                                    milesByProgram = mapOf(prog1 to throughTotal),
                                    highlightBadge = if (throughCabin == CabinClass.SUITES) "1 PNR SQ + A380 Suites" else saveText,
                                    explanation = "KrisFlyer own-metal Singapore Airlines (1 PNR) via $hubIata (${hubAirport.city}) dihitung berdasarkan total jarak gabungan ${formatMiles(combinedDist * mult)} mi$mixCabinNote. ${throughEst.ruleSummary}.",
                                    transferHub = hubIata
                                )
                            )
                        } else {
                            // Non-SQ KrisFlyer, GarudaMiles, and Asia Miles: calculated per leg
                            val perLegLabel = when (prog1) {
                                MilesProgram.KRISFLYER -> "KF non-SQ dihitung per leg (${formatMiles(splitTotal)} mi)"
                                MilesProgram.GARUDA_MILES -> "GarudaMiles dihitung per leg (${formatMiles(splitTotal)} mi)"
                                MilesProgram.ASIA_MILES -> "Asia Miles dihitung per leg (${formatMiles(splitTotal)} mi)"
                            }
                            sameAllianceCandidates.add(
                                ItineraryRecommendation(
                                    id = "SAMES_${r1.airlineCode}_${hubIata}_${r2.airlineCode}",
                                    type = ItineraryType.SAME_ALLIANCE_SPLIT,
                                    legs = listOf(legPlan1, legPlan2),
                                    totalMiles = splitTotal,
                                    totalDistanceMi = combinedDist * mult,
                                    milesByProgram = mapOf(prog1 to splitTotal),
                                    highlightBadge = perLegLabel,
                                    explanation = "Untuk ${prog1.displayName} (${r1.airlineCode} + ${r2.airlineCode} via $hubIata), kebutuhan miles dihitung per leg: Leg 1 ${formatMiles(m1)} mi + Leg 2 ${formatMiles(m2)} mi = ${formatMiles(splitTotal)} mi.",
                                    transferHub = hubIata
                                )
                            )
                        }
                    } else {
                        // CROSS-ALLIANCE MIX MILES!
                        val legPlan1 = LegPlan(r1, al1Name, origin.city, hubAirport.city, prog1, m1, est1.tierLabel, c1)
                        val legPlan2 = LegPlan(r2, al2Name, hubAirport.city, dest.city, prog2, m2, est2.tierLabel, c2)
                        val progMap = mapOf(prog1 to m1, prog2 to m2)
                        val affordable = wallet.canAfford(progMap)
                        val badge = when {
                            affordable -> "Saldo Mix Cukup!"
                            c1 == CabinClass.SUITES || c2 == CabinClass.SUITES -> "Mix + A380 Suites"
                            c1 == CabinClass.ARIA_SUITE || c2 == CabinClass.ARIA_SUITE -> "Mix + CX Aria Suite"
                            else -> "Mix ${prog1.shortCode} + ${prog2.shortCode}"
                        }
                        crossAllianceCandidates.add(
                            ItineraryRecommendation(
                                id = "MIX_${r1.airlineCode}_${hubIata}_${r2.airlineCode}",
                                type = ItineraryType.CROSS_ALLIANCE_MIX,
                                legs = listOf(legPlan1, legPlan2),
                                totalMiles = splitTotal,
                                totalDistanceMi = combinedDist * mult,
                                milesByProgram = progMap,
                                highlightBadge = badge,
                                explanation = "Strategi lintas aliansi via $hubIata (${hubAirport.city}): Leg 1 pakai ${prog1.displayName} (${formatMiles(m1)} mi, ${r1.airlineCode}) + Leg 2 pakai ${prog2.displayName} (${formatMiles(m2)} mi, ${r2.airlineCode}). Ideal untuk menghabiskan saldo miles terpisah (2 PNR terpisah).",
                                transferHub = hubIata
                            )
                        )
                    }
                }
            }
        }

        // Pick top diverse same-alliance options (up to 4 per program)
        val topSameAlliance = sameAllianceCandidates
            .groupBy { it.milesByProgram.keys.first() }
            .values
            .flatMap { list ->
                list.sortedWith(compareBy<ItineraryRecommendation> { it.totalMiles }.thenBy { it.totalDistanceMi })
                    .distinctBy { it.transferHub }
                    .take(4)
            }

        // Pick top diverse cross-alliance mix options (up to 4 per program pair!)
        val topCrossAlliance = crossAllianceCandidates
            .groupBy { rec ->
                val keys = rec.milesByProgram.keys.sortedBy { it.ordinal }
                "${keys[0].shortCode}_${keys[1].shortCode}"
            }
            .values
            .flatMap { list ->
                list.sortedWith(
                    compareByDescending<ItineraryRecommendation> { wallet.canAfford(it.milesByProgram) }
                        .thenBy { it.totalMiles }
                        .thenBy { it.totalDistanceMi }
                )
                    .distinctBy { it.transferHub }
                    .take(5)
            }

        val allRecs = (results + topSameAlliance + topCrossAlliance)
            .sortedWith(
                compareBy<ItineraryRecommendation> { it.type == ItineraryType.CROSS_ALLIANCE_MIX && !wallet.canAfford(it.milesByProgram) }
                    .thenBy { it.totalMiles }
                    .thenBy { it.totalDistanceMi }
            )

        val bestSingle = allRecs
            .filter { it.type != ItineraryType.CROSS_ALLIANCE_MIX }
            .minByOrNull { it.totalMiles }

        val bestMix = topCrossAlliance
            .minWithOrNull(
                compareByDescending<ItineraryRecommendation> { wallet.canAfford(it.milesByProgram) }
                    .thenBy { it.totalMiles }
            )

        return MixSearchResult(
            originIata = originIata,
            destIata = destIata,
            directDistanceMi = directDist * mult,
            cabin = cabin,
            isRoundTrip = isRoundTrip,
            recommendations = allRecs,
            bestSingleAlliance = bestSingle,
            bestCrossAllianceMix = bestMix
        )
    }

    private fun pickBestPerProgram(
        routes: List<FlightRoute>,
        requestedCabin: CabinClass,
        isLongLeg: Boolean
    ): Map<MilesProgram, Pair<FlightRoute, CabinClass>> {
        val result = HashMap<MilesProgram, Pair<FlightRoute, CabinClass>>()
        for (prog in MilesProgram.entries) {
            val candidates = routes.filter { it.program == prog }
            if (candidates.isEmpty()) continue
            var best: Pair<FlightRoute, CabinClass>? = null
            var bestScore = Int.MAX_VALUE
            for (r in candidates) {
                val resolved = resolveCabinForLeg(r, requestedCabin, allowFallback = !isLongLeg || requestedCabin == CabinClass.SUITES || requestedCabin == CabinClass.ARIA_SUITE)
                    ?: continue
                val exactMatchPenalty = if (resolved == requestedCabin) 0 else 50
                val flagshipBonus = when (r.airlineCode) {
                    "SQ", "GA", "CX" -> -20
                    "NH", "JL", "QR", "KL", "AF", "KE", "LH", "BA", "QF", "BR", "CI", "TK" -> -10
                    else -> 0
                }
                val score = exactMatchPenalty + flagshipBonus
                if (score < bestScore) {
                    bestScore = score
                    best = r to resolved
                }
            }
            if (best != null) {
                result[prog] = best
            }
        }
        return result
    }

    private fun resolveCabinForLeg(
        route: FlightRoute,
        requested: CabinClass,
        allowFallback: Boolean
    ): CabinClass? {
        if (route.cabins.contains(requested.code)) return requested
        if (!allowFallback) return null
        return when (requested) {
            CabinClass.SUITES -> when {
                route.cabins.contains("F") -> CabinClass.FIRST
                route.cabins.contains("J") -> CabinClass.BUSINESS
                else -> null
            }
            CabinClass.FIRST -> when {
                route.cabins.contains("J") -> CabinClass.BUSINESS
                else -> null
            }
            CabinClass.PREMIUM_ECONOMY -> when {
                route.cabins.contains("Y") -> CabinClass.ECONOMY
                route.cabins.contains("J") -> CabinClass.BUSINESS
                else -> null
            }
            CabinClass.ARIA_SUITE -> when {
                route.cabins.contains("J") -> CabinClass.BUSINESS
                route.cabins.contains("Y") -> CabinClass.ECONOMY
                else -> null
            }
            CabinClass.BUSINESS -> if (route.cabins.contains("Y")) CabinClass.ECONOMY else null
            CabinClass.ECONOMY -> if (route.cabins.contains("Y")) CabinClass.ECONOMY else null
        }
    }
}
