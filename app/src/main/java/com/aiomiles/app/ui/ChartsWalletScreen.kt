package com.aiomiles.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiomiles.app.data.MilesProgram
import com.aiomiles.app.data.formatMiles
import com.aiomiles.app.ui.theme.AsiaMilesColor
import com.aiomiles.app.ui.theme.GarudaColor
import com.aiomiles.app.ui.theme.KrisFlyerColor

@Composable
fun ChartsWalletScreen(
    state: UiState,
    viewModel: MainViewModel
) {
    var gmInput by remember(state.wallet) { mutableStateOf(if (state.wallet.garudaMiles > 0) state.wallet.garudaMiles.toString() else "") }
    var kfInput by remember(state.wallet) { mutableStateOf(if (state.wallet.krisFlyer > 0) state.wallet.krisFlyer.toString() else "") }
    var amInput by remember(state.wallet) { mutableStateOf(if (state.wallet.asiaMiles > 0) state.wallet.asiaMiles.toString() else "") }
    var savedBanner by remember { mutableStateOf(false) }
    var chartTab by remember { mutableStateOf(MilesProgram.KRISFLYER) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. WALLET BALANCE CARD
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Dompet Saldo Miles Saya",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Masukkan saldo miles Anda agar Kalkulator otomatis menandai rute & saran Mix Beda Aliansi yang sudah cukup untuk di-redeem.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = gmInput,
                        onValueChange = { gmInput = it.filter { ch -> ch.isDigit() }; savedBanner = false },
                        label = { Text("Saldo GarudaMiles (SkyTeam)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = kfInput,
                        onValueChange = { kfInput = it.filter { ch -> ch.isDigit() }; savedBanner = false },
                        label = { Text("Saldo KrisFlyer (Star Alliance)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amInput,
                        onValueChange = { amInput = it.filter { ch -> ch.isDigit() }; savedBanner = false },
                        label = { Text("Saldo Asia Miles (oneworld)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))

                    val totalCombined = (gmInput.toIntOrNull() ?: 0) + (kfInput.toIntOrNull() ?: 0) + (amInput.toIntOrNull() ?: 0)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Gabungan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${formatMiles(totalCombined)} miles", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Button(onClick = {
                            viewModel.updateWallet(
                                gm = gmInput.toIntOrNull() ?: 0,
                                kf = kfInput.toIntOrNull() ?: 0,
                                am = amInput.toIntOrNull() ?: 0
                            )
                            savedBanner = true
                        }) {
                            Text("Simpan Saldo")
                        }
                    }
                    if (savedBanner) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Saldo tersimpan & disinkronkan ke Kalkulator Mix Miles.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }
        }

        // 2. AWARD CHARTS REFERENCE
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Referensi Award Chart (Sekali Jalan)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MilesProgram.entries.forEach { prog ->
                            FilterChip(
                                selected = chartTab == prog,
                                onClick = { chartTab = prog },
                                label = { Text(prog.displayName, fontSize = 12.sp) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    when (chartTab) {
                        MilesProgram.GARUDA_MILES -> {
                            ChartHeader("Band Jarak (SkyTeam Partner)", GarudaColor)
                            ChartRow("≤ 500 mi", "10.000", "20.000", "30.000")
                            ChartRow("501 – 1.000 mi", "15.000", "30.000", "45.000")
                            ChartRow("1.001 – 1.500 mi", "20.000", "40.000", "60.000")
                            ChartRow("1.501 – 2.500 mi", "27.500", "55.000", "82.500")
                            ChartRow("2.501 – 3.500 mi", "35.000", "70.000", "105.000")
                            ChartRow("3.501 – 5.000 mi", "45.000", "90.000", "135.000")
                            ChartRow("5.001 – 7.000 mi", "55.000", "110.000", "165.000")
                            ChartRow("7.001 – 9.000 mi", "65.000", "130.000", "195.000")
                            ChartRow("9.001+ mi", "80.000", "160.000", "240.000")
                        }
                        MilesProgram.KRISFLYER -> {
                            ChartHeader("Dari Indonesia (Zona 2) — SQ Saver", KrisFlyerColor)
                            ChartRow("Ke Singapura (Z1)", "8.500", "21.000", "30.500")
                            ChartRow("Ke SE Asia Lain (Z3)", "13.500", "24.000", "33.500")
                            ChartRow("Ke HKG / TPE (Z4)", "16.500", "34.000", "45.000")
                            ChartRow("Ke PVG / PEK (Z5)", "21.500", "43.000", "58.500*")
                            ChartRow("Ke Jepang / Korea (Z7)", "27.000", "52.000", "77.000")
                            ChartRow("Ke SYD / MEL / AKL (Z9)", "30.500", "68.500", "93.500*")
                            ChartRow("Ke Timur Tengah (Z10)", "29.000", "56.500", "85.000")
                            ChartRow("Ke Eropa (Z11)", "42.000", "103.500", "141.000*")
                            ChartRow("Ke AS Pantai Barat (Z12)", "44.000", "107.000", "148.500")
                            ChartRow("Ke AS Pantai Timur (Z13)", "44.000", "111.500", "148.500*")
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "*Tersedia SQ A380 Suites pada rute tertentu (SIN–LHR, SIN–SYD, SIN–FRA–JFK, SIN–PVG, SIN–HKG, SIN–DEL/BOM).",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        MilesProgram.ASIA_MILES -> {
                            ChartHeader("Band Jarak (Cathay Pacific Standard)", AsiaMilesColor)
                            ChartRow("Ultra-Short ≤750 mi", "7.500", "16.000", "25.000")
                            ChartRow("Short 751–2.750 mi", "12.500", "28.000", "45.000")
                            ChartRow("Medium 2.751–5.000 mi", "20.000", "58.000", "87.000")
                            ChartRow("Long 5.001–7.500 mi", "27.000", "84.000", "125.000")
                            ChartRow("Ultra-Long 7.501+ mi", "38.000", "110.000", "160.000")
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Catatan: Mitra oneworld tunggal (JL, QR, BA, QF, MH, dll.) menggunakan tarif partner (~10–15% di atas CX). Multi-Carrier oneworld berlaku untuk 2+ maskapai oneworld.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartHeader(title: String, color: Color) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = color)
    Spacer(Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("Zona / Jarak", modifier = Modifier.weight(1.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Eco", modifier = Modifier.weight(0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Biz", modifier = Modifier.weight(0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("First/S", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun ChartRow(zone: String, y: String, j: String, f: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(zone, modifier = Modifier.weight(1.7f), fontSize = 12.sp)
        Text(y, modifier = Modifier.weight(0.9f), fontSize = 12.sp)
        Text(j, modifier = Modifier.weight(0.9f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(f, modifier = Modifier.weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}
