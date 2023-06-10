package com.example.grafoprediopuc

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class MainActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var fromEditText: EditText
    private lateinit var toEditText: EditText
    private lateinit var calculateButton: Button
    private lateinit var resultTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicialização do Firebase Firestore
        db = FirebaseFirestore.getInstance()

        fromEditText = findViewById(R.id.sourceEditText)
        toEditText = findViewById(R.id.destinationEditText)
        calculateButton = findViewById(R.id.calculateButton)
        resultTextView = findViewById(R.id.shortestPathTextView)

        calculateButton.setOnClickListener { calculateShortestPath() }
    }

    private fun calculateShortestPath() {
        val from = fromEditText.text.toString()
        val to = toEditText.text.toString()

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
                    val shortestPath = dijkstra(buildingsMap, from, to)

                    // Exibição do resultado na TextView
                    resultTextView.text = "Caminho mais rápido: $shortestPath"
                } else {
                    // Tratamento de erro
                    resultTextView.text = "Erro ao buscar os dados do Firestore."
                }
            }
    }

    private fun dijkstra(buildingsMap: HashMap<String, HashMap<String, Int>>, from: String, to: String): List<String> {
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
}
