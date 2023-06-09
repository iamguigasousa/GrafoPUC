package com.example.grafoprediopuc

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.util.PriorityQueue



class MainActivity : AppCompatActivity() {
    data class Node(val name: String)

    data class Edge(val source: Node, val destination: Node, val time: Int)

    class Graph {
        private val nodes = mutableListOf<Node>()
        private val edges = mutableListOf<Edge>()

        fun addNode(node: Node) {
            nodes.add(node)
        }

        fun addEdge(edge: Edge) {
            edges.add(edge)
        }

        fun menorCaminho(source: Node, destination: Node): List<Node>? {
            val distances = mutableMapOf<Node, Int>()
            val previous = mutableMapOf<Node, Node>()
            val queue = PriorityQueue<Node>(Comparator.comparingInt { distances.getOrDefault(it, Int.MAX_VALUE) })

            for (node in nodes) {
                if (node == source) {
                    distances[node] = 0
                } else {
                    distances[node] = Int.MAX_VALUE
                }
                previous[node] = null!!
                queue.add(node)
            }

            while (queue.isNotEmpty()) {
                val current = queue.poll()

                if (current == destination) {
                    val path = mutableListOf<Node>()
                    var node = current
                    while (node != null) {
                        path.add(0, node)
                        node = previous[node]
                    }
                    return path
                }

                for (edge in edges.filter { it.source == current }) {
                    val time = distances[current]!! + edge.time
                    if (time < distances[edge.destination]!!) {
                        distances[edge.destination] = time
                        previous[edge.destination] = current
                        queue.remove(edge.destination)
                        queue.add(edge.destination)
                    }
                }
            }

            return null
        }
    }








    private lateinit var db: FirebaseFirestore
    private lateinit var graph: Graph




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()
        graph = Graph()




        val buttonFindPath = findViewById<Button>(R.id.btn_find_path)
        val textResult = findViewById<TextView>(R.id.text_result)
        val etOrigem = findViewById<EditText>(R.id.et_source)
        val etDestino = findViewById<EditText>(R.id.et_destination)

        buttonFindPath.setOnClickListener {
            val origem = Node(etOrigem.toString()) // Altere para o prédio de origem selecionado
            val destino = Node(etDestino.toString()) // Altere para o prédio de destino selecionado

            val menorCaminho = graph.menorCaminho(origem, destino)

            if (menorCaminho != null) {
                val result = StringBuilder()
                for (node in menorCaminho) {
                    result.append(node.name).append(" -> ")
                }
                result.delete(result.length - 4, result.length)
                textResult.text = result.toString()
            } else {
                textResult.text = "Caminho não encontrado"
            }
        }

        fetchGraphData()
    }

    private fun fetchGraphData() {
        db.collection("prédios").get()
            .addOnSuccessListener { prediosSnapshot ->
                for (predioDocument in prediosSnapshot.documents) {
                    val predioName = predioDocument.id
                    val predio = Node(predioName)
                    graph.addNode(predio)

                    val caminhosCollection = db.document(predioDocument.id).collection("caminhos")
                    caminhosCollection.get()
                        .addOnSuccessListener { caminhosSnapshot ->
                            for (caminhoDocument in caminhosSnapshot.documents) {
                                val destinoName = caminhoDocument.id
                                val tempo = caminhoDocument.getLong("tempo")?.toInt()
                                if (tempo != null) {
                                    val destino = Node(destinoName)
                                    val caminho = Edge(predio, destino, tempo)
                                    graph.addEdge(caminho)
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            // Tratar falha na recuperação dos caminhos
                        }
                }
            }
            .addOnFailureListener { exception ->
                // Tratar falha na recuperação dos prédios
            }
    }
}
