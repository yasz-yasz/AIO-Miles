package com.aiomiles.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiomiles.app.data.formatMiles
import com.aiomiles.app.ui.AirlinesScreen
import com.aiomiles.app.ui.CalculatorScreen
import com.aiomiles.app.ui.ChartsWalletScreen
import com.aiomiles.app.ui.MainTab
import com.aiomiles.app.ui.MainViewModel
import com.aiomiles.app.ui.RoutesExplorerScreen
import com.aiomiles.app.ui.theme.AIOMilesTheme
import com.aiomiles.app.ui.theme.NavyPrimary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AIOMilesTheme {
                AIOMilesApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIOMilesApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AIO Miles",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp
                        )
                        val subtitle = state.db?.let {
                            "${it.airlines.size} Maskapai • ${formatMiles(it.routes.size)} Rute • 3 Aliansi"
                        } ?: "Memuat database rute global..."
                        Text(
                            text = subtitle,
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary)
            )
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    val icon = when (tab) {
                        MainTab.CALCULATOR -> Icons.Default.CompareArrows
                        MainTab.ROUTES -> Icons.Default.Public
                        MainTab.AIRLINES -> Icons.Default.AirplanemodeActive
                        MainTab.CHARTS -> Icons.Default.AccountBalanceWallet
                    }
                    NavigationBarItem(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        icon = { Icon(icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Memuat 12.000+ rute penerbangan global...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                when (state.selectedTab) {
                    MainTab.CALCULATOR -> CalculatorScreen(state, viewModel)
                    MainTab.ROUTES -> RoutesExplorerScreen(state, viewModel)
                    MainTab.AIRLINES -> AirlinesScreen(state, viewModel)
                    MainTab.CHARTS -> ChartsWalletScreen(state, viewModel)
                }
            }
        }
    }
}
