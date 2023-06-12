package com.example.grafoprediopuc

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.grafoprediopuc.databinding.ActivityMainBinding
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class MainActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialização do Firebase Firestore
        db = FirebaseFirestore.getInstance()

        binding.btnCalculate.setOnClickListener { calculateShortestPathAndTime() }
    }

    private fun calculateShortestPathAndTime() {
        val source = binding.etSource.text.toString().uppercase()
        val destination = binding.etDestination.text.toString().uppercase()

        // Consulta ao Firestore para obter todos os prédios
        db.collection("prédios")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val documents: QuerySnapshot? = task.result
                    val buildingsMap = HashMap<String, HashMap<String, Int>>()

                    for (document: DocumentSnapshot in documents!!) {
                        val buildingName = document.id
                        val adjacentBuildings = document.data as HashMap<String, Int>
                        buildingsMap[buildingName] = adjacentBuildings
                    }

                    // Chamada da função para calcular o caminho mais curto
                    try{
                        val shortestPath = calculateDijkstra(buildingsMap, source, destination)
                        val totalTime = calculateTravelTime(buildingsMap, source, destination)

                        // Exibição do resultado na TextView
                        binding.tvShortestPath.text = "Caminho mais rápido: $shortestPath e o tempo a ser percorrido: $totalTime min"
                    } catch (e: Exception) {
                        binding.tvShortestPath.text = "Não é possível chegar até o local informado."
                    }

                } else {
                    // Tratamento de erro
                    binding.tvShortestPath.text = "Erro ao buscar os dados do Firestore."
                }
            }
    }

    private fun calculateDijkstra(buildingsMap: HashMap<String, HashMap<String, Int>>, from: String, to: String): List<String> {
        val distances = HashMap<String, Int>()
        val previous = HashMap<String, String>()
        val unvisited = HashSet<String>()

        for (building in buildingsMap.keys) {
            distances[building] = Int.MAX_VALUE
            previous[building] = ""
            unvisited.add(building)
        }

        distances[from] = 0

        while (unvisited.isNotEmpty()) {
            var currentBuilding = ""
            var shortestDistance = Int.MAX_VALUE

            for (building in unvisited) {
                if (distances[building]!! < shortestDistance) {
                    shortestDistance = distances[building]!!
                    currentBuilding = building
                }
            }

            if (currentBuilding == to) {
                break
            }

            unvisited.remove(currentBuilding)

            val adjacentBuildings = buildingsMap[currentBuilding]
            if (adjacentBuildings != null) {
                for (adjacentBuilding in adjacentBuildings.keys) {
                    val distance = distances[currentBuilding]!! + adjacentBuildings[adjacentBuilding]!!

                    if (distance < distances[adjacentBuilding]!!) {
                        distances[adjacentBuilding] = distance
                        previous[adjacentBuilding] = currentBuilding
                    }
                }
            }
        }

        val shortestPath = mutableListOf<String>()
        var currentBuilding = to
        while (currentBuilding != "") {
            shortestPath.add(0, currentBuilding)
            currentBuilding = previous[currentBuilding]!!
        }

        return shortestPath
    }
     private fun calculateTravelTime(buildingsMap: HashMap<String, HashMap<String, Int>>, source: String, destination: String): Int? {
        val distances = HashMap<String, Int>()
        val previous = HashMap<String, String>()
        val unvisited = HashSet<String>()

        for (building in buildingsMap.keys) {
            distances[building] = Int.MAX_VALUE
            previous[building] = ""
            unvisited.add(building)
        }

        distances[source] = 0

        while (unvisited.isNotEmpty()) {
            var currentBuilding = ""
            var shortestDistance = Int.MAX_VALUE

            for (building in unvisited) {
                if (distances[building]!! < shortestDistance) {
                    shortestDistance = distances[building]!!
                    currentBuilding = building
                }
            }

            if (currentBuilding == destination) {
                break
            }

            unvisited.remove(currentBuilding)

            val adjacentBuildings = buildingsMap[currentBuilding]
            if (adjacentBuildings != null) {
                for (adjacentBuilding in adjacentBuildings.keys) {
                    val distance = distances[currentBuilding]!! + adjacentBuildings[adjacentBuilding]!!

                    if (distance < distances[adjacentBuilding]!!) {
                        distances[adjacentBuilding] = distance
                        previous[adjacentBuilding] = currentBuilding
                    }
                }
            }
        }

        if (distances[destination] == Int.MAX_VALUE) {
            return null
        }

        val travelTime = distances[destination]!!
        return travelTime
    }


}
