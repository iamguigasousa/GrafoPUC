package com.example.grafoprediopuc

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var startBuildingEditText: EditText
    private lateinit var endBuildingEditText: EditText
    private lateinit var calculateButton: Button
    private lateinit var resultTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firestore = FirebaseFirestore.getInstance()

        startBuildingEditText = findViewById(R.id.sourceEditText)
        endBuildingEditText = findViewById(R.id.destinationEditText)
        calculateButton = findViewById(R.id.calculateButton)
        resultTextView = findViewById(R.id.shortestPathTextView)

        calculateButton.setOnClickListener {
            val startBuilding = startBuildingEditText.text.toString()
            val endBuilding = endBuildingEditText.text.toString()

            if (startBuilding.isNotEmpty() && endBuilding.isNotEmpty()) {
                calculateShortestPath(startBuilding, endBuilding)
            }
        }
    }

    private fun calculateShortestPath(startBuilding: String, endBuilding: String) {
        val buildingsCollection = firestore.collection("prédios")

        buildingsCollection.document(startBuilding).get()
            .addOnSuccessListener { startBuildingDocument ->
                if (startBuildingDocument != null && startBuildingDocument.exists()) {
                    val startBuildingData = startBuildingDocument.data
                    val adjacentBuildings = startBuildingData?.keys?.toList() ?: emptyList()
                    val visitedBuildings = mutableSetOf(startBuilding)
                    val distances = mutableMapOf(startBuilding to 0)
                    val previousBuildings = mutableMapOf<String, String>()

                    adjacentBuildings.forEach { building ->
                        if (building != startBuilding) {
                            distances[building] = Int.MAX_VALUE
                        }
                    }

                    while (visitedBuildings.size < adjacentBuildings.size) {
                        var currentBuilding = ""
                        var minDistance = Int.MAX_VALUE

                        distances.forEach { (building, distance) ->
                            if (distance < minDistance && !visitedBuildings.contains(building)) {
                                currentBuilding = building
                                minDistance = distance
                            }
                        }

                        if (currentBuilding.isEmpty()) {
                            break
                        }

                        visitedBuildings.add(currentBuilding)
                        buildingsCollection.document(currentBuilding).get()
                            .addOnSuccessListener { currentBuildingDocument ->
                                if (currentBuildingDocument != null && currentBuildingDocument.exists()) {
                                    val currentBuildingData = currentBuildingDocument.data
                                    val currentBuildingAdjacent = currentBuildingData?.keys?.toList()
                                        ?: emptyList()

                                    currentBuildingAdjacent.forEach { adjacent ->
                                        val distance = currentBuildingData?.get(adjacent) as? Int
                                        if (distance != null) {
                                            val totalDistance = minDistance + distance
                                            if (totalDistance < (distances[adjacent] ?: Int.MAX_VALUE)) {
                                                distances[adjacent] = totalDistance
                                                previousBuildings[adjacent] = currentBuilding
                                            }
                                        }
                                    }
                                }
                            }
                    }

                    if (distances.containsKey(endBuilding)) {
                        val shortestPath = mutableListOf<String>()
                        var currentBuilding = endBuilding

                        while (currentBuilding != startBuilding) {
                            shortestPath.add(0, currentBuilding)
                            currentBuilding = previousBuildings[currentBuilding] ?: ""
                        }

                        shortestPath.add(0, startBuilding)
                        val totalTime = distances[endBuilding]
                        val result = "Shortest Path: ${shortestPath.joinToString(" -> ")}, Time: $totalTime minutes"
                        resultTextView.text = result
                    } else {
                        resultTextView.text = "No path found."
                    }
                } else {
                    resultTextView.text = "Start building not found."
                }
            }
    }
}
