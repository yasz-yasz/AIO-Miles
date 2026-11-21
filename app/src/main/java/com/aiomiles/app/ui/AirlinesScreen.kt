package com.aiomiles.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiomiles.app.data.Airline
import com.aiomiles.app.data.formatMiles
import com.aiomiles.app.ui.theme.AsiaMilesColor
import com.aiomiles.app.ui.theme.GarudaColor
import com.aiomiles.app.ui.theme.KrisFlyerColor

@Composable
fun AirlinesScreen(
    state: UiState,
    viewModel: MainViewModel
) {
    val db = state.db ?: return
    var query by remember { mutableStateOf("") }
    var selectedAlliance by remember { mutableStateOf("Semua") }

    val filtered = remember(db.airlines, query, selectedAlliance) {
        val q = query.trim().lowercase()
        db.airlines.filter { al ->
            val alOk = when (selectedAlliance) {
                "Semua" -> true
                "Mitra Non-Aliansi" -> al.alliance.startsWith("Mitra")
                else -> al.alliance.equals(selectedAlliance, ignoreCase = true)
            }
            val qOk = q.isEmpty() ||
                al.name.lowercase().contains(q) ||
                al.code.lowercase().contains(q) ||
                al.country.lowercase().contains(q) ||
                al.hubs.any { it.lowercase().contains(q) }
            alOk && qOk
        }
    }

    val skyCount = remember(db.airlines) { db.airlines.count { it.alliance == "SkyTeam" } }
    val starCount = remember(db.airlines) { db.airlines.count { it.alliance == "Star Alliance" } }
    val owCount = remember(db.airlines) { db.airlines.count { it.alliance == "oneworld" } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Overview 3 Alliances Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AllianceSummaryTile("SkyTeam", "$skyCount Maskapai", "GarudaMiles", GarudaColor, Modifier.weight(1f))
                AllianceSummaryTile("Star Alliance", "$starCount Maskapai", "KrisFlyer", KrisFlyerColor, Modifier.weight(1f))
                AllianceSummaryTile("oneworld", "$owCount Maskapai", "Asia Miles", AsiaMilesColor, Modifier.weight(1f))
            }
        }

        // Search & Filter
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Cari maskapai, kode IATA, hub, atau negara...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Semua", "SkyTeam", "Star Alliance", "oneworld", "Mitra Non-Aliansi").forEach { item ->
                    FilterChip(
                        selected = selectedAlliance == item,
                        onClick = { selectedAlliance = item },
                        label = { Text(item, fontSize = 12.sp) }
                    )
                }
            }
        }

        items(filtered, key = { it.code }) { airline ->
            val routeCount = db.routeCountByAirline[airline.code] ?: 0
            AirlineCard(
                airline = airline,
                routeCount = routeCount,
                onViewRoutes = { viewModel.filterByAirlineAndOpenRoutes(airline.code) }
            )
        }
    }
}

@Composable
private fun AllianceSummaryTile(
    title: String,
    countLabel: String,
    program: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(2.dp))
            Text(countLabel, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(program, color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun AirlineCard(
    airline: Airline,
    routeCount: Int,
    onViewRoutes: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewRoutes)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(airline.program.colorHex),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = airline.code,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(airline.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "${airline.alliance} • ${airline.country} • Hub: ${airline.hubs.take(3).joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${formatMiles(routeCount)} rute",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(airline.program.colorHex),
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = airline.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                airline.cabins.forEach { c ->
                    val label = when (c) {
                        "Y" -> "Eco"
                        "W" -> "Prem Eco"
                        "J" -> "Biz"
                        "F" -> "First"
                        "S" -> "A380 Suites"
                        "A" -> "Aria Suite"
                        else -> c
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
