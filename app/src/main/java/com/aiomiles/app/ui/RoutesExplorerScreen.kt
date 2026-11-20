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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiomiles.app.data.CabinClass
import com.aiomiles.app.data.FlightRoute
import com.aiomiles.app.data.LoadedDatabase
import com.aiomiles.app.data.formatMiles
import com.aiomiles.app.engine.AwardCalculator
import com.aiomiles.app.ui.theme.NavyPrimary

@Composable
fun RoutesExplorerScreen(
    state: UiState,
    viewModel: MainViewModel
) {
    val db = state.db ?: return
    var selectedRouteForDetail by remember { mutableStateOf<FlightRoute?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Stats & Search
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Menampilkan ${formatMiles(state.filteredRoutes.size)} dari ${formatMiles(db.routes.size)} rute",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (state.routeAirlineFilter != null) {
                        Surface(
                            color = NavyPrimary,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable { viewModel.clearAirlineFilter() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Maskapai: ${state.routeAirlineFilter}",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Hapus filter",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.routeQuery,
                    onValueChange = { viewModel.updateRouteQuery(it) },
                    placeholder = { Text("Cari rute, kota, bandara, atau maskapai (CGK, Tokyo, SQ...)", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.routeQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateRouteQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Bersihkan")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(6.dp))

                // Alliance + Special filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Semua", "SkyTeam", "Star Alliance", "oneworld", "Mitra Non-Aliansi").forEach { al ->
                        FilterChip(
                            selected = state.routeAllianceFilter == al,
                            onClick = { viewModel.updateRouteAlliance(al) },
                            label = { Text(al, fontSize = 12.sp) }
                        )
                    }
                    FilterChip(
                        selected = state.routeOnlyIndonesia,
                        onClick = { viewModel.toggleOnlyIndonesia() },
                        label = { Text("Indonesia", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = state.routeOnlyFifthFreedom,
                        onClick = { viewModel.toggleOnlyFifthFreedom() },
                        label = { Text("Fifth Freedom", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = state.routeOnlySuites,
                        onClick = { viewModel.toggleOnlySuites() },
                        label = { Text("A380 Suites", fontSize = 12.sp) }
                    )
                }

                // Cabin preview selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Estimasi:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CabinClass.entries.forEach { cabin ->
                        FilterChip(
                            selected = state.routePreviewCabin == cabin,
                            onClick = { viewModel.setRoutePreviewCabin(cabin) },
                            label = { Text(cabin.shortLabel, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Virtualized 12,000+ Routes LazyColumn
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = state.filteredRoutes,
                key = { "${it.origin}_${it.dest}_${it.airlineCode}" }
            ) { route ->
                RouteRowCard(
                    route = route,
                    db = db,
                    previewCabin = state.routePreviewCabin,
                    onClick = { selectedRouteForDetail = route }
                )
            }
        }
    }

    selectedRouteForDetail?.let { route ->
        RouteDetailDialog(
            route = route,
            db = db,
            onDismiss = { selectedRouteForDetail = null },
            onOpenInCalculator = {
                selectedRouteForDetail = null
                viewModel.setRoutePairAndCalculate(route.origin, route.dest, state.routePreviewCabin)
            }
        )
    }
}

@Composable
private fun RouteRowCard(
    route: FlightRoute,
    db: LoadedDatabase,
    previewCabin: CabinClass,
    onClick: () -> Unit
) {
    val originAp = db.airportMap[route.origin]
    val destAp = db.airportMap[route.dest]
    val airline = db.airlineMap[route.airlineCode]

    val hasRequestedCabin = route.cabins.contains(previewCabin.code)
    val estMiles = remember(route, previewCabin) {
        if (originAp != null && destAp != null && hasRequestedCabin) {
            AwardCalculator.calculateOneWay(
                program = route.program,
                airlineCode = route.airlineCode,
                origin = originAp,
                dest = destAp,
                distanceMi = route.distanceMi,
                cabin = previewCabin
            ).miles
        } else null
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(route.program.colorHex),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = route.airlineCode,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${route.origin} → ${route.dest}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (route.isFifthFreedom) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "5th Freedom",
                                color = Color(0xFF92400E),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (route.cabins.contains("S")) {
                        Spacer(Modifier.width(5.dp))
                        Surface(
                            color = Color(0xFFFCE7F3),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Suites",
                                color = Color(0xFF9D174D),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${originAp?.city ?: route.origin} → ${destAp?.city ?: route.dest}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${airline?.name ?: route.airlineCode} • ${route.alliance} • ${formatMiles(route.distanceMi)} mi",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (estMiles != null) {
                    Text(
                        text = formatMiles(estMiles),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(route.program.colorHex),
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${route.program.shortCode} (${previewCabin.shortLabel})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "N/A",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = route.cabins.joinToString("/"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteDetailDialog(
    route: FlightRoute,
    db: LoadedDatabase,
    onDismiss: () -> Unit,
    onOpenInCalculator: () -> Unit
) {
    val originAp = db.airportMap[route.origin]
    val destAp = db.airportMap[route.dest]
    val airline = db.airlineMap[route.airlineCode]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("${route.origin} → ${route.dest}", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${airline?.name ?: route.airlineCode} (${route.alliance})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${originAp?.city}, ${originAp?.country} → ${destAp?.city}, ${destAp?.country}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Jarak: ${formatMiles(route.distanceMi)} mil (${formatMiles(route.distanceKm)} km) • Program: ${route.program.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                Text(
                    text = "Estimasi Redeem Sekali Jalan (1-Way):",
                    style = MaterialTheme.typography.titleSmall
                )
                if (originAp != null && destAp != null) {
                    CabinClass.entries.forEach { cabin ->
                        if (route.cabins.contains(cabin.code)) {
                            val est = AwardCalculator.calculateOneWay(
                                program = route.program,
                                airlineCode = route.airlineCode,
                                origin = originAp,
                                dest = destAp,
                                distanceMi = route.distanceMi,
                                cabin = cabin
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cabin.label, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${formatMiles(est.miles)} mi",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(route.program.colorHex)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onOpenInCalculator) {
                Icon(Icons.Default.Flight, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Cek Mix Miles")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
