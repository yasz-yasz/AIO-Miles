package com.aiomiles.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiomiles.app.data.CabinClass
import com.aiomiles.app.data.FlightRoute
import com.aiomiles.app.data.LoadedDatabase
import com.aiomiles.app.data.MilesRepository
import com.aiomiles.app.data.MilesWallet
import com.aiomiles.app.data.WalletStorage
import com.aiomiles.app.engine.MixSearchResult
import com.aiomiles.app.engine.RouteMixEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MainTab(val title: String) {
    CALCULATOR("Kalkulator"),
    ROUTES("12K+ Rute"),
    AIRLINES("Maskapai"),
    CHARTS("Chart & Saldo")
}

data class UiState(
    val isLoading: Boolean = true,
    val db: LoadedDatabase? = null,
    val selectedTab: MainTab = MainTab.CALCULATOR,
    // Calculator State
    val originIata: String = "CGK",
    val destIata: String = "LHR",
    val selectedCabin: CabinClass = CabinClass.BUSINESS,
    val isRoundTrip: Boolean = false,
    val useKfAdvantage: Boolean = false,
    val isCalculating: Boolean = false,
    val searchResult: MixSearchResult? = null,
    // Routes Explorer State
    val routeQuery: String = "",
    val routeAllianceFilter: String = "Semua",
    val routeAirlineFilter: String? = null,
    val routeOnlyIndonesia: Boolean = false,
    val routeOnlyFifthFreedom: Boolean = false,
    val routeOnlySuites: Boolean = false,
    val routeOnlyAriaSuite: Boolean = false,
    val routePreviewCabin: CabinClass = CabinClass.BUSINESS,
    val filteredRoutes: List<FlightRoute> = emptyList(),
    // Wallet
    val wallet: MilesWallet = MilesWallet()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MilesRepository(application)
    private val walletStorage = WalletStorage(application)

    private val _uiState = MutableStateFlow(UiState(wallet = walletStorage.load()))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = repository.loadDatabase()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                db = loaded,
                filteredRoutes = loaded.routes
            )
            runCalculation()
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun setOrigin(iata: String) {
        _uiState.value = _uiState.value.copy(originIata = iata)
        runCalculation()
    }

    fun setDest(iata: String) {
        _uiState.value = _uiState.value.copy(destIata = iata)
        runCalculation()
    }

    fun swapAirports() {
        val s = _uiState.value
        _uiState.value = s.copy(originIata = s.destIata, destIata = s.originIata)
        runCalculation()
    }

    fun setRoutePairAndCalculate(origin: String, dest: String, cabin: CabinClass? = null) {
        val s = _uiState.value
        _uiState.value = s.copy(
            originIata = origin,
            destIata = dest,
            selectedCabin = cabin ?: s.selectedCabin,
            selectedTab = MainTab.CALCULATOR
        )
        runCalculation()
    }

    fun setCabin(cabin: CabinClass) {
        _uiState.value = _uiState.value.copy(selectedCabin = cabin)
        runCalculation()
    }

    fun setRoundTrip(roundTrip: Boolean) {
        _uiState.value = _uiState.value.copy(isRoundTrip = roundTrip)
        runCalculation()
    }

    fun setKfAdvantage(advantage: Boolean) {
        _uiState.value = _uiState.value.copy(useKfAdvantage = advantage)
        runCalculation()
    }

    fun runCalculation() {
        val s = _uiState.value
        val db = s.db ?: return
        _uiState.value = s.copy(isCalculating = true)
        viewModelScope.launch {
            val res = withContext(Dispatchers.Default) {
                RouteMixEngine.search(
                    db = db,
                    originIata = s.originIata,
                    destIata = s.destIata,
                    cabin = s.selectedCabin,
                    isRoundTrip = s.isRoundTrip,
                    useKfAdvantage = s.useKfAdvantage,
                    wallet = s.wallet
                )
            }
            _uiState.value = _uiState.value.copy(isCalculating = false, searchResult = res)
        }
    }

    fun updateWallet(gm: Int, kf: Int, am: Int) {
        val updated = MilesWallet(garudaMiles = gm, krisFlyer = kf, asiaMiles = am)
        walletStorage.save(updated)
        _uiState.value = _uiState.value.copy(wallet = updated)
        runCalculation()
    }

    // Routes Explorer Filters
    fun updateRouteQuery(q: String) {
        _uiState.value = _uiState.value.copy(routeQuery = q)
        applyRouteFilters()
    }

    fun updateRouteAlliance(alliance: String) {
        _uiState.value = _uiState.value.copy(routeAllianceFilter = alliance, routeAirlineFilter = null)
        applyRouteFilters()
    }

    fun filterByAirlineAndOpenRoutes(airlineCode: String) {
        _uiState.value = _uiState.value.copy(
            routeAirlineFilter = airlineCode,
            routeAllianceFilter = "Semua",
            routeQuery = "",
            routeOnlyIndonesia = false,
            routeOnlyFifthFreedom = false,
            routeOnlySuites = false,
            routeOnlyAriaSuite = false,
            selectedTab = MainTab.ROUTES
        )
        applyRouteFilters()
    }

    fun clearAirlineFilter() {
        _uiState.value = _uiState.value.copy(routeAirlineFilter = null)
        applyRouteFilters()
    }

    fun toggleOnlyIndonesia() {
        _uiState.value = _uiState.value.copy(routeOnlyIndonesia = !_uiState.value.routeOnlyIndonesia)
        applyRouteFilters()
    }

    fun toggleOnlyFifthFreedom() {
        _uiState.value = _uiState.value.copy(routeOnlyFifthFreedom = !_uiState.value.routeOnlyFifthFreedom)
        applyRouteFilters()
    }

    fun toggleOnlySuites() {
        _uiState.value = _uiState.value.copy(routeOnlySuites = !_uiState.value.routeOnlySuites)
        applyRouteFilters()
    }

    fun toggleOnlyAriaSuite() {
        _uiState.value = _uiState.value.copy(routeOnlyAriaSuite = !_uiState.value.routeOnlyAriaSuite)
        applyRouteFilters()
    }

    fun setRoutePreviewCabin(cabin: CabinClass) {
        _uiState.value = _uiState.value.copy(routePreviewCabin = cabin)
    }

    private fun applyRouteFilters() {
        val s = _uiState.value
        val db = s.db ?: return
        viewModelScope.launch {
            val filtered = withContext(Dispatchers.Default) {
                val q = s.routeQuery.trim().lowercase()
                val indoAirports = setOf(
                    "CGK", "DPS", "SUB", "KNO", "UPG", "YIA", "SRG", "BPN",
                    "MDC", "PLM", "PKU", "PDG", "LOP", "BTH", "PNK", "BDJ", "SOC", "AMQ", "DJJ", "LBJ"
                )
                db.routes.filter { r ->
                    if (s.routeAirlineFilter != null && r.airlineCode != s.routeAirlineFilter) return@filter false
                    if (s.routeAllianceFilter != "Semua") {
                        val ok = when (s.routeAllianceFilter) {
                            "Mitra Non-Aliansi" -> r.alliance.startsWith("Mitra")
                            else -> r.alliance.equals(s.routeAllianceFilter, ignoreCase = true)
                        }
                        if (!ok) return@filter false
                    }
                    if (s.routeOnlyIndonesia && r.origin !in indoAirports && r.dest !in indoAirports) return@filter false
                    if (s.routeOnlyFifthFreedom && !r.isFifthFreedom) return@filter false
                    if (s.routeOnlySuites && !r.cabins.contains("S")) return@filter false
                    if (s.routeOnlyAriaSuite && !r.cabins.contains("A")) return@filter false
                    if (q.isNotEmpty()) {
                        val oAp = db.airportMap[r.origin]
                        val dAp = db.airportMap[r.dest]
                        val al = db.airlineMap[r.airlineCode]
                        val match = r.origin.lowercase().contains(q) ||
                            r.dest.lowercase().contains(q) ||
                            "${r.origin}-${r.dest}".lowercase().contains(q) ||
                            r.airlineCode.lowercase().contains(q) ||
                            (oAp?.city?.lowercase()?.contains(q) == true) ||
                            (dAp?.city?.lowercase()?.contains(q) == true) ||
                            (oAp?.country?.lowercase()?.contains(q) == true) ||
                            (dAp?.country?.lowercase()?.contains(q) == true) ||
                            (al?.name?.lowercase()?.contains(q) == true)
                        if (!match) return@filter false
                    }
                    true
                }
            }
            _uiState.value = _uiState.value.copy(filteredRoutes = filtered)
        }
    }
}
