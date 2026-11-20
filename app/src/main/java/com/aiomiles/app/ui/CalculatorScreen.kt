package com.aiomiles.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aiomiles.app.data.Airport
import com.aiomiles.app.data.CabinClass
import com.aiomiles.app.data.ItineraryRecommendation
import com.aiomiles.app.data.ItineraryType
import com.aiomiles.app.data.MilesWallet
import com.aiomiles.app.data.formatMiles
import com.aiomiles.app.ui.theme.GoldAccent
import com.aiomiles.app.ui.theme.NavyPrimary

@Composable
fun CalculatorScreen(
    state: UiState,
    viewModel: MainViewModel
) {
    val db = state.db ?: return
    var pickingForOrigin by remember { mutableStateOf<Boolean?>(null) }
    var activeFilter by remember { mutableStateOf("Semua") }
    var onlyAffordable by remember { mutableStateOf(false) }

    val originAp = db.airportMap[state.originIata]
    val destAp = db.airportMap[state.destIata]
    val result = state.searchResult

    val displayedRecs = remember(result, activeFilter, onlyAffordable, state.wallet) {
        val base = result?.recommendations.orEmpty()
        base.filter { rec ->
            val typeOk = when (activeFilter) {
                "Direct" -> rec.type == ItineraryType.DIRECT
                "Sama Aliansi" -> rec.type == ItineraryType.SAME_ALLIANCE_SINGLE || rec.type == ItineraryType.SAME_ALLIANCE_SPLIT
                "Mix Beda Aliansi" -> rec.type == ItineraryType.CROSS_ALLIANCE_MIX
                else -> true
            }
            val walletOk = !onlyAffordable || state.wallet.canAfford(rec.milesByProgram)
            typeOk && walletOk
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. ROUTE & CABIN INPUT CARD
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AirportSelectorBox(
                            label = "Asal",
                            iata = state.originIata,
                            city = originAp?.city ?: "",
                            isOrigin = true,
                            modifier = Modifier.weight(1f),
                            onClick = { pickingForOrigin = true }
                        )
                        IconButton(
                            onClick = { viewModel.swapAirports() },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Tukar bandara",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        AirportSelectorBox(
                            label = "Tujuan",
                            iata = state.destIata,
                            city = destAp?.city ?: "",
                            isOrigin = false,
                            modifier = Modifier.weight(1f),
                            onClick = { pickingForOrigin = false }
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Popular Quick Routes
                    val presets = listOf(
                        "CGK" to "LHR",
                        "CGK" to "NRT",
                        "CGK" to "JFK",
                        "CGK" to "PVG",
                        "CGK" to "CDG",
                        "SIN" to "SYD",
                        "CGK" to "IST"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { (o, d) ->
                            val selected = state.originIata == o && state.destIata == d
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (selected) NavyPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { viewModel.setRoutePairAndCalculate(o, d) }
                            ) {
                                Text(
                                    text = "$o→$d",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Cabin selector
                    Text(
                        text = "Kelas Kabin",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CabinClass.entries.forEach { cabin ->
                            FilterChip(
                                selected = state.selectedCabin == cabin,
                                onClick = { viewModel.setCabin(cabin) },
                                label = { Text(cabin.shortLabel, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Trip Type & KrisFlyer Tier Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !state.isRoundTrip,
                            onClick = { viewModel.setRoundTrip(false) },
                            label = { Text("Sekali Jalan", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = state.isRoundTrip,
                            onClick = { viewModel.setRoundTrip(true) },
                            label = { Text("Pulang-Pergi (PP)", fontSize = 12.sp) }
                        )
                        Spacer(Modifier.weight(1f))
                        FilterChip(
                            selected = state.useKfAdvantage,
                            onClick = { viewModel.setKfAdvantage(!state.useKfAdvantage) },
                            label = { Text(if (state.useKfAdvantage) "KF: Adv" else "KF: Saver", fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        // 2. SMART SUMMARY BANNER (Best Single vs Best Mix Cross-Alliance)
        if (result != null && result.recommendations.isNotEmpty()) {
            item {
                SmartComparisonBanner(
                    directDistMi = result.directDistanceMi,
                    bestSingle = result.bestSingleAlliance,
                    bestMix = result.bestCrossAllianceMix,
                    wallet = state.wallet
                )
            }
        }

        // 3. RESULT CATEGORY FILTER CHIPS
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tabs = listOf("Semua", "Direct", "Sama Aliansi", "Mix Beda Aliansi")
                tabs.forEach { tab ->
                    FilterChip(
                        selected = activeFilter == tab,
                        onClick = { activeFilter = tab },
                        label = { Text(tab, fontSize = 12.sp) }
                    )
                }
                if (state.wallet.hasAnyBalance()) {
                    FilterChip(
                        selected = onlyAffordable,
                        onClick = { onlyAffordable = !onlyAffordable },
                        label = { Text("Saldo Cukup", fontSize = 12.sp) }
                    )
                }
            }
        }

        // 4. RESULTS LIST
        if (state.isCalculating) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (displayedRecs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Belum ada opsi rute untuk filter ini",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Coba ubah kelas kabin (misal Business/Economy) atau pilih filter 'Semua'. Untuk Suites (A380), rute tersedia melalui hub SIN (SQ).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(displayedRecs, key = { it.id }) { rec ->
                RecommendationCard(rec = rec, wallet = state.wallet)
            }
        }
    }

    if (pickingForOrigin != null) {
        AirportPickerDialog(
            title = if (pickingForOrigin == true) "Pilih Bandara Asal" else "Pilih Bandara Tujuan",
            airports = db.airports,
            onSelect = { ap ->
                if (pickingForOrigin == true) viewModel.setOrigin(ap.iata)
                else viewModel.setDest(ap.iata)
                pickingForOrigin = null
            },
            onDismiss = { pickingForOrigin = null }
        )
    }
}

@Composable
private fun AirportSelectorBox(
    label: String,
    iata: String,
    city: String,
    isOrigin: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isOrigin) Icons.Default.FlightTakeoff else Icons.Default.FlightLand,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = iata,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = city,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SmartComparisonBanner(
    directDistMi: Int,
    bestSingle: ItineraryRecommendation?,
    bestMix: ItineraryRecommendation?,
    wallet: MilesWallet
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyPrimary)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ringkasan Saran Miles",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
                Text(
                    text = "Jarak ~${formatMiles(directDistMi)} mi",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFCBD5E1)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Single Alliance Best
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF1A364E),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Termurah 1 Aliansi",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF93C5FD)
                        )
                        Spacer(Modifier.height(2.dp))
                        if (bestSingle != null) {
                            val prog = bestSingle.milesByProgram.keys.first()
                            Text(
                                text = "${formatMiles(bestSingle.totalMiles)} mi",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "${prog.displayName} (${bestSingle.legs.joinToString("+") { it.route.airlineCode }})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE2E8F0),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text("Tidak tersedia", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                }

                // Cross Alliance Mix Best
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF1A364E),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Saran Mix Beda Aliansi",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFBBF24)
                        )
                        Spacer(Modifier.height(2.dp))
                        if (bestMix != null) {
                            Text(
                                text = "${formatMiles(bestMix.totalMiles)} mi",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            val mixLabel = bestMix.milesByProgram.entries.joinToString(" + ") { (p, m) ->
                                "${p.shortCode} ${formatMiles(m)}"
                            }
                            Text(
                                text = mixLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (wallet.canAfford(bestMix.milesByProgram)) Color(0xFF4ADE80) else Color(0xFFE2E8F0),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text("Cukup 1 aliansi", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    rec: ItineraryRecommendation,
    wallet: MilesWallet
) {
    var expanded by remember { mutableStateOf(false) }
    val isMix = rec.type == ItineraryType.CROSS_ALLIANCE_MIX
    val affordable = wallet.canAfford(rec.milesByProgram)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Type badge + Total Miles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val badgeBg = when (rec.type) {
                        ItineraryType.DIRECT -> Color(0xFFDCFCE7)
                        ItineraryType.SAME_ALLIANCE_SINGLE -> Color(0xFFDBEAFE)
                        ItineraryType.SAME_ALLIANCE_SPLIT -> Color(0xFFE0E7FF)
                        ItineraryType.CROSS_ALLIANCE_MIX -> Color(0xFFFEF3C7)
                    }
                    val badgeFg = when (rec.type) {
                        ItineraryType.DIRECT -> Color(0xFF166534)
                        ItineraryType.SAME_ALLIANCE_SINGLE -> Color(0xFF1E40AF)
                        ItineraryType.SAME_ALLIANCE_SPLIT -> Color(0xFF3730A3)
                        ItineraryType.CROSS_ALLIANCE_MIX -> Color(0xFF92400E)
                    }
                    Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isMix) {
                                Icon(
                                    Icons.Default.CompareArrows,
                                    contentDescription = null,
                                    tint = badgeFg,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                            }
                            Text(
                                text = rec.type.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeFg,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${formatMiles(rec.totalMiles)} miles",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${formatMiles(rec.totalDistanceMi)} mi",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Highlight Chip + Wallet status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rec.highlightBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
                if (wallet.hasAnyBalance()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (affordable) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (affordable) Color(0xFF15803D) else Color(0xFFB45309),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = wallet.deficitSummary(rec.milesByProgram),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (affordable) Color(0xFF15803D) else Color(0xFFB45309)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Legs breakdown
            rec.legs.forEachIndexed { idx, leg ->
                if (idx > 0) {
                    Spacer(Modifier.height(6.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(leg.program.colorHex),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = leg.route.airlineCode,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${leg.route.origin} → ${leg.route.dest}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "• ${leg.cabinUsed.shortLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "${leg.airlineName} (${leg.program.displayName})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val showLegMiles = rec.type != ItineraryType.SAME_ALLIANCE_SINGLE
                        Text(
                            text = if (showLegMiles) "${formatMiles(leg.miles)} mi" else "Termasuk 1 PNR",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(leg.program.colorHex)
                        )
                        Text(
                            text = "${formatMiles(leg.route.distanceMi)} mi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Miles per program pills for Cross-Alliance Mix
            if (rec.milesByProgram.size > 1) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rec.milesByProgram.forEach { (prog, amount) ->
                        Surface(
                            color = Color(prog.colorHex).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = prog.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(prog.colorHex)
                                )
                                Text(
                                    text = "${formatMiles(amount)} miles",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(prog.colorHex)
                                )
                            }
                        }
                    }
                }
            }

            // Expandable detail explanation
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Sembunyikan detail" else "Lihat analisis & strategi",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = rec.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun AirportPickerDialog(
    title: String,
    airports: List<Airport>,
    onSelect: (Airport) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, airports) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            // Put popular gateways first
            val popular = listOf("CGK", "DPS", "SUB", "SIN", "KUL", "HKG", "BKK", "NRT", "HND", "ICN", "PVG", "PEK", "TPE", "SYD", "MEL", "DOH", "DXB", "IST", "AMS", "CDG", "LHR", "FRA", "JFK", "LAX", "SFO")
            val popSet = popular.toSet()
            val top = popular.mapNotNull { code -> airports.firstOrNull { it.iata == code } }
            val rest = airports.filter { it.iata !in popSet }
            top + rest
        } else {
            airports.filter {
                it.iata.lowercase().contains(q) ||
                    it.city.lowercase().contains(q) ||
                    it.country.lowercase().contains(q)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Cari IATA, kota, atau negara (mis. CGK, Tokyo)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filtered, key = { it.iata }) { ap ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(ap) }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = ap.iata,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ap.city, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(ap.country, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "Z${ap.kfZone}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }
}
