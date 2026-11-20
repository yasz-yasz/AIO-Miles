package com.aiomiles.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class LoadedDatabase(
    val airports: List<Airport>,
    val airportMap: Map<String, Airport>,
    val airlines: List<Airline>,
    val airlineMap: Map<String, Airline>,
    val routes: List<FlightRoute>,
    val outgoingByAirport: Map<String, List<FlightRoute>>,
    val routeCountByAirline: Map<String, Int>
)

class MilesRepository(private val context: Context) {

    suspend fun loadDatabase(): LoadedDatabase = withContext(Dispatchers.IO) {
        val airports = ArrayList<Airport>(350)
        val airportMap = HashMap<String, Airport>(400)
        context.assets.open("airports.tsv").use { stream ->
            BufferedReader(InputStreamReader(stream)).forEachLine { line ->
                if (line.isBlank()) return@forEachLine
                val p = line.split('	')
                if (p.size >= 6) {
                    val ap = Airport(
                        iata = p[0],
                        city = p[1],
                        country = p[2],
                        kfZone = p[3].toIntOrNull() ?: 11,
                        lat = p[4].toDoubleOrNull() ?: 0.0,
                        lon = p[5].toDoubleOrNull() ?: 0.0
                    )
                    airports.add(ap)
                    airportMap[ap.iata] = ap
                }
            }
        }

        val airlines = ArrayList<Airline>(80)
        val airlineMap = HashMap<String, Airline>(80)
        context.assets.open("airlines.tsv").use { stream ->
            BufferedReader(InputStreamReader(stream)).forEachLine { line ->
                if (line.isBlank()) return@forEachLine
                val p = line.split('	')
                if (p.size >= 8) {
                    val al = Airline(
                        code = p[0],
                        name = p[1],
                        alliance = p[2],
                        program = MilesProgram.fromString(p[3]),
                        hubs = p[4].split(',').filter { it.isNotBlank() },
                        cabins = p[5].split(',').filter { it.isNotBlank() }.toSet(),
                        country = p[6],
                        note = p[7]
                    )
                    airlines.add(al)
                    airlineMap[al.code] = al
                }
            }
        }

        val routes = ArrayList<FlightRoute>(13000)
        val outgoing = HashMap<String, MutableList<FlightRoute>>(400)
        val airlineCount = HashMap<String, Int>(80)

        context.assets.open("routes_db.tsv").use { stream ->
            BufferedReader(InputStreamReader(stream)).forEachLine { line ->
                if (line.isBlank()) return@forEachLine
                val p = line.split('	')
                if (p.size >= 8) {
                    val route = FlightRoute(
                        origin = p[0],
                        dest = p[1],
                        airlineCode = p[2],
                        alliance = p[3],
                        program = MilesProgram.fromString(p[4]),
                        distanceMi = p[5].toIntOrNull() ?: 500,
                        cabins = p[6].split(',').filter { it.isNotBlank() }.toSet(),
                        isFifthFreedom = p[7] == "1"
                    )
                    routes.add(route)
                    outgoing.getOrPut(route.origin) { ArrayList() }.add(route)
                    airlineCount[route.airlineCode] = (airlineCount[route.airlineCode] ?: 0) + 1
                }
            }
        }

        LoadedDatabase(
            airports = airports,
            airportMap = airportMap,
            airlines = airlines,
            airlineMap = airlineMap,
            routes = routes,
            outgoingByAirport = outgoing,
            routeCountByAirline = airlineCount
        )
    }
}
