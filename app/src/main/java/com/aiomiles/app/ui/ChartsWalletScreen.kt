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

        // 2. AWARD CHARTS REFERENCE (UPDATED 2026)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Referensi Award Chart 2026 (Sekali Jalan)",
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
                            ChartHeader("Garuda Own-Metal Internasional (Apr 2026, 1-Way)", GarudaColor)
                            ChartRow("CGK–KUL / SUB–SIN", "12.500", "22.500", "43.200")
                            ChartRow("CGK–SIN", "14.500", "28.000", "43.200")
                            ChartRow("DPS–SIN", "15.000", "27.000", "49.500")
                            ChartRow("CGK–BKK", "18.500", "33.500", "49.500")
                            ChartRow("CGK–HKG / CAN", "25.000", "45.000", "77.700")
                            ChartRow("CGK–PVG / DPS–SYD,MEL", "30.000", "54.000", "90.000")
                            ChartRow("CGK/DPS–ICN,HND,NRT", "35.000", "63.000", "108.000")
                            ChartRow("CGK–SYD / MEL", "42.000", "74.000", "108.000")
                            ChartRow("CGK–JED / MED / DOH", "49.000", "89.000", "126.000")
                            ChartRow("CGK–AMS", "50.000", "110.000", "190.000")
                            Spacer(Modifier.height(10.dp))
                            ChartHeader("Garuda Own-Metal Domestik Utama (Apr 2026, 1-Way)", GarudaColor)
                            ChartRow("SUB–DPS / CGK–TKG", "4.800", "7.700–13.000", "12.700–25.400")
                            ChartRow("CGK–SRG / SOC / PLM / PNK", "9.600", "15.400", "25.400")
                            ChartRow("CGK–YIA / SUB", "10.500–11.500", "17.000–19.000", "25.400")
                            ChartRow("CGK–BTH / PDG / PKU / LOP", "16.800", "27.500", "45.300")
                            ChartRow("CGK–DPS", "17.900", "32.000", "45.300")
                            ChartRow("CGK–BPN / LBJ / KNO / UPG", "21.600–23.500", "35.200–38.000", "58.000")
                            ChartRow("CGK–BTJ / KOE / MDC / AMQ", "28.800", "42.500–47.300", "78.000")
                            ChartRow("CGK–TTE / SOQ", "33.600", "49.500–55.000", "90.700")
                            ChartRow("CGK–DJJ / TIM / MKQ", "40.800", "67.100", "110.700")
                            Spacer(Modifier.height(10.dp))
                            ChartHeader("SkyTeam / Partner Chart (Group A–I, 1-Way)", GarudaColor)
                            ChartRow("Group A (1–1.000 mi)", "15.000", "25.000", "37.500")
                            ChartRow("Group B (1.001–1.500 mi)", "17.500", "30.000", "45.000")
                            ChartRow("Group C (1.501–2.000 mi)", "22.500", "37.500", "56.500")
                            ChartRow("Group D (2.001–2.500 mi)", "27.500", "47.500", "71.500")
                            ChartRow("Group E (2.501–3.000 mi)", "32.500", "57.500", "86.500")
                            ChartRow("Group F (3.001–4.000 mi)", "37.500", "67.500", "101.500")
                            ChartRow("Group G (4.001–6.000 mi)", "42.500", "75.000", "112.500")
                            ChartRow("Group H (6.001–8.000 mi)", "52.500", "95.000", "142.500")
                            ChartRow("Group I (8.001–10.000+ mi)", "57.500", "100.000", "150.000")
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Sesuai Tabel Penukaran Miles Garuda Indonesia (start Apr 2026) untuk Own-Metal (Economy X / Business O / First R) dan terpisah dari chart SkyTeam/Partner.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        MilesProgram.KRISFLYER -> {
                            ChartHeader("Dari Singapura (Zona 1) — SQ Saver (1-Way)", KrisFlyerColor)
                            ChartRow("Ke Z1 / Z2 (ID, MY, BN)", "8.000", "22.000", "32.000")
                            ChartRow("Ke Z3 (TH, VN, PH, KH, LA, MM)", "13.000", "25.000", "38.000")
                            ChartRow("Ke Z4 (HKG, TPE, China Sel.)", "15.500", "35.500", "47.500")
                            ChartRow("Ke Z5 (PVG, PEK)", "20.500", "45.000", "61.500*")
                            ChartRow("Ke Z6 (Asia Selatan)", "19.000", "45.000", "61.500*")
                            ChartRow("Ke Z7 (Jepang, Korea Selatan)", "25.500", "54.500", "81.000")
                            ChartRow("Ke Z8 (Perth, Darwin)", "20.500", "42.500", "60.500")
                            ChartRow("Ke Z9 (SYD, MEL, AKL, dll.)", "29.000", "72.000", "98.000*")
                            ChartRow("Ke Z10 (Timur Tengah, Afrika, TR)", "32.000", "68.000", "95.000")
                            ChartRow("Ke Z11 (Eropa)", "44.000", "108.500", "148.000*")
                            ChartRow("Ke Z12 (AS Pantai Barat)", "44.000", "112.500", "154.000")
                            ChartRow("Ke Z13 (AS Pantai Timur)", "46.000", "117.000", "156.000*")
                            Spacer(Modifier.height(10.dp))
                            ChartHeader("Dari Indonesia (Zona 2) — SQ Saver (1-Way)", KrisFlyerColor)
                            ChartRow("Ke Singapura (Z1)", "8.000", "22.000", "32.000")
                            ChartRow("Ke Z2 / Z3 (MY, BN / SE Asia)", "8.500–13.500", "23.000–26.500", "33.500–39.500")
                            ChartRow("Ke Z4 (HKG, TPE, China Sel.)", "16.000", "36.500", "48.500")
                            ChartRow("Ke Z5 (PVG, PEK)", "21.000", "46.500", "63.000")
                            ChartRow("Ke Z6 (Asia Selatan)", "20.000", "47.500", "64.500")
                            ChartRow("Ke Z7 (Jepang, Korea Selatan)", "27.000", "57.000", "84.500")
                            ChartRow("Ke Z8 (Perth, Darwin)", "21.500", "44.500", "63.500")
                            ChartRow("Ke Z9 (SYD, MEL, AKL, dll.)", "30.500", "75.500", "103.000")
                            ChartRow("Ke Z10 (Timur Tengah, Afrika, TR)", "33.500", "70.500", "99.500")
                            ChartRow("Ke Z11 (Eropa)", "46.000", "114.000", "155.000")
                            ChartRow("Ke Z12 (AS Pantai Barat)", "46.000", "117.500", "161.000")
                            ChartRow("Ke Z13 (AS Pantai Timur)", "48.500", "122.500", "163.500")
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Mengacu pada Singapore Airlines One-Way Saver & Advantage Awards Chart (efektif 1 Nov 2025). Gunakan toggle KF: Saver / Adv di Kalkulator untuk tarif Advantage (mis. SIN–Z2: 16.500 Eco / 40.500 Biz / 60.500 Suites; Z2–Z11: 82.500 Eco / 147.500 Biz / 270.500 Suites).",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        MilesProgram.ASIA_MILES -> {
                            ChartHeader("Cathay Pacific Standard Award Chart (1-Way)", AsiaMilesColor)
                            ChartRow("Ultra-Short (1–750 mi)", "7.000", "16.000", "27.000")
                            ChartRow("Short Type 1 (751–2.750 mi)", "9.000", "28.000", "43.000")
                            ChartRow("Short Type 2 (751–2.750 mi)*", "13.000", "33.000", "50.000")
                            ChartRow("Medium-Haul (2.751–5.000 mi)", "20.000", "58.000", "85.000")
                            ChartRow("Long-Haul (5.001–7.500 mi)", "27.000", "88.000", "125.000")
                            ChartRow("Ultra-Long (7.501+ mi)", "38.000", "115.000", "160.000")
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Tarif Premium Economy CX: 15.000 (Ultra-Short), 18.000 (Short Type 1), 23.000 (Short Type 2), 40.000 (Medium), 50.000 (Long), 75.000 (Ultra-Long). *Short Type 2 berlaku untuk rute dari/ke Indonesia, Jepang, dan Asia Selatan (India, Nepal, Bangladesh, Sri Lanka). Kolom Biz mencakup Aria Suite B777-300ER.",
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
