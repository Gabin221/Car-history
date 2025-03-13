package com.example.carhistory

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.GridLabelRenderer
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import org.json.JSONArray
import org.json.JSONException
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var buttonAccount: TextView
    private lateinit var titrePage: TextView
    private lateinit var buttonLeftCar: TextView
    private lateinit var buttonRightCar: TextView
    private lateinit var buttonCenterCar: TextView
    private lateinit var buttonAddPlein: TextView
    private lateinit var lineGraphView: GraphView
    private lateinit var imageCar: ImageView
    private lateinit var textBuyDate: TextView
    private lateinit var textDistRun: TextView
    private lateinit var buttonSearchGasStation: TextView
    private lateinit var textCoordinates: TextView
    private lateinit var textMean: TextView
    private lateinit var distanceTotalParcourue: TextView
    private lateinit var recordConso: TextView
    private lateinit var distanceCarParcourue: TextView

    private var listeDates = mutableListOf<String>()
    private var listeDistances = mutableListOf<String>()
    private var listeVolumes = mutableListOf<String>()
    private var recordConsoValue = 0.0
    private var currentCarName = ""
    private var currentCarDist = 0.0

    private val logoData = listOf(
        Pair(R.drawable.account_box_outline, "buttonAccount"),
        Pair(R.drawable.map_search_outline, "buttonSearchGasStation"),
        Pair(R.drawable.plus_circle_outline, "buttonAddPlein")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        buttonAccount = findViewById(R.id.buttonAccount)
        titrePage = findViewById(R.id.titrePage)
        buttonLeftCar = findViewById(R.id.buttonLeftCar)
        buttonRightCar = findViewById(R.id.buttonRightCar)
        buttonCenterCar = findViewById(R.id.buttonCenterCar)
        buttonAddPlein = findViewById(R.id.buttonAddPlein)
        lineGraphView = findViewById(R.id.lineGraphView)
        imageCar = findViewById(R.id.imageCar)
        textBuyDate = findViewById(R.id.textBuyDate)
        textDistRun = findViewById(R.id.textDistRun)
        buttonSearchGasStation = findViewById(R.id.buttonSearchGasStation)
        textMean = findViewById(R.id.textMean)

        buttonAccount.setOnClickListener {
            val inflater = this.layoutInflater
            val dialogView = inflater.inflate(R.layout.stats_window_modale, null)
            distanceTotalParcourue = dialogView.findViewById<TextView>(R.id.distanceTotalParcourue)
            recordConso = dialogView.findViewById<TextView>(R.id.recordConso)
            distanceCarParcourue = dialogView.findViewById<TextView>(R.id.distanceCarParcourue)

            recupererInfosConducteur(distanceTotalParcourue)

            recordConso.text = "${"%.3f".format(recordConsoValue)} L/100km en record de consommation avec la $currentCarName"
            distanceCarParcourue.text = "${"%.0f".format(currentCarDist)} km parcourus avec la $currentCarName"

            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder
                .setTitle("Statistiques")
                .setView(dialogView)

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }

        buttonLeftCar.setOnClickListener {
            startFunctions(ValuesManager.currentIDCar - 1)
            ValuesManager.currentIDCar--
        }

        buttonRightCar.setOnClickListener {
            startFunctions(ValuesManager.currentIDCar + 1)
            ValuesManager.currentIDCar++
        }

        buttonSearchGasStation.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val inflater = LayoutInflater.from(this)
            val dialogView = inflater.inflate(R.layout.formulaire_find_station, null)

            val editTextDistance = dialogView.findViewById<EditText>(R.id.editTextDistance)
            val listViewCarburants = dialogView.findViewById<ListView>(R.id.listViewCarburants)

            val carburants = listOf("B7", "SP95-E5", "SP95-E10", "SP98-E5", "E85", "LPG")
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, carburants)
            listViewCarburants.adapter = adapter
            listViewCarburants.choiceMode = ListView.CHOICE_MODE_MULTIPLE

            builder
                .setView(dialogView)
                .setPositiveButton("Rechercher") { _, _ ->
                    val distanceKm = editTextDistance.text.toString().toDoubleOrNull() ?: 0.0
                    val distanceMetres = (distanceKm * 1000).toInt()

                    val selectedCarburants = mutableListOf<String>()
                    for (i in 0 until listViewCarburants.count) {
                        if (listViewCarburants.isItemChecked(i)) {
                            selectedCarburants.add(carburants[i])
                        }
                    }

                    if (selectedCarburants.isEmpty()) {
                        Toast.makeText(this, "Veuillez sÃ©lectionner au moins un carburant", Toast.LENGTH_SHORT).show()
                    } else {
                        val carburantsQuery = selectedCarburants.joinToString(",")

                        recupererFindGasStation(this, distanceMetres, carburantsQuery)
                    }
                }
                .setNegativeButton("Annuler") { dialog, _ -> dialog.dismiss() }

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }

        buttonAddPlein.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.formulaire_ajout_plein, null)

            builder
                .setView(dialogView)
                .setPositiveButton("Ajouter") { dialog, which ->
                    val editTextVolume = dialogView.findViewById<EditText>(R.id.editTextVolume)
                    val editTextDistance = dialogView.findViewById<EditText>(R.id.editTextDistance)

                    val volume = editTextVolume.text.toString().toDoubleOrNull() ?: 0.0
                    val distance = editTextDistance.text.toString().toDoubleOrNull() ?: 0.0

                    ajouterPlein(ValuesManager.currentIDCar, volume, distance)
                    modifierDistances(ValuesManager.currentIDCar, distance)
                }
                .setNegativeButton("Abandonner") { dialog, which ->
                    Toast.makeText(this, "Le plein ne sera pas ajoutÃ©.", Toast.LENGTH_SHORT).show()
                }

            val dialog: AlertDialog = builder.create()

            dialog.setOnShowListener {
                dialog.window?.setBackgroundDrawableResource(R.color.background_card_add_plein)

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE)
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED)
            }

            dialog.show()
        }

        initLogo()
        getCurrentCar()
        startFunctions(ValuesManager.currentIDCar)
    }

    fun initLogo() {
        logoData.forEach { (drawableId, textViewId) ->
            val textView = findViewById<TextView>(resources.getIdentifier(textViewId, "id", packageName))
            val text = ""
            val spannableString = SpannableString(" $text ")
            val drawable = ContextCompat.getDrawable(this, drawableId)
            drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            val imageSpan = ImageSpan(drawable!!, ImageSpan.ALIGN_BOTTOM)
            spannableString.setSpan(imageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            textView.text = spannableString
        }
    }

    private fun startFunctions(valeur_id: Int) {
        recupererInfosVoitures(valeur_id)
        recupererDonneesGraphe(valeur_id)
    }

    private fun recupererInfosVoitures(valeur_id: Int) {
        val queue = Volley.newRequestQueue(this)
        val url = "use/your/script/recupererInfos.php?valeur_id=$valeur_id"

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                val parts = response.split(";")
                val titre = parts[0] + " " + parts[1]
                val imageUrl = parts[2]
                val dateAchat = parts[3]
                currentCarDist = parts[4].toDouble()
                currentCarName = titre
                titrePage.text = currentCarName
                buttonCenterCar.text = titre
                textBuyDate.text = dateAchat
                textDistRun.text = "${"%.0f".format(currentCarDist)} km"

                if (imageUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(imageUrl)
                        .into(imageCar)
                }

            },
            { error ->
                Log.e("Volley", "Erreur de requÃªte : ${error.message}")
                Toast.makeText(this, "ProblÃ¨me de rÃ©cupÃ©ration des infos", Toast.LENGTH_SHORT).show()
            })

        queue.add(stringRequest)
    }

    private fun ajouterPlein(valeur_id: Int, volume: Double, distance: Double) {
        val date = getCurrentDate()
        val queue = Volley.newRequestQueue(this)
        val url = "use/your/script/ajouterPlein.php?valeur_id=$valeur_id&volume=$volume&distance=$distance&date=$date"

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                Toast.makeText(this, "Le plein a Ã©tÃ© ajoutÃ© avec succÃ¨s.", Toast.LENGTH_SHORT).show()
            },
            { error ->
                Log.e("Volley", "Erreur de requÃªte : ${error.message}")
                Toast.makeText(this, "ProblÃ¨me lors de l'ajout du plein.", Toast.LENGTH_SHORT).show()
            })

        queue.add(stringRequest)
    }

    private fun recupererDonneesGraphe(valeur_id: Int) {
        val url = "use/your/script/recupererPleins.php?valeur_id=$valeur_id"

        val queue = Volley.newRequestQueue(this)
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val listeDataPoints = traiterDonnees(response)
                    graphe1(listeDataPoints)
                } catch (e: Exception) {
                    Log.e("Volley", "Erreur traitement : ${e.message}")
                }
            },
            { error ->
                Log.e("Volley", "Erreur requÃªte : ${error.networkResponse?.statusCode} - ${error.message}")
            }
        )

        queue.add(stringRequest)
    }

    private fun modifierDistances(valeur_id: Int, distance: Double) {
        val queue = Volley.newRequestQueue(this)
        val url = "use/your/script/modifierDistance.php?valeur_id=$valeur_id&distance=$distance"

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                Toast.makeText(this, "Les distances ont Ã©tÃ© mises Ã  jour avec succÃ¨s.", Toast.LENGTH_SHORT).show()
            },
            { error ->
                Log.e("Volley", "Erreur de requÃªte : ${error.message}")
                Toast.makeText(this, "ProblÃ¨me lors de la mise Ã  jour des distances.", Toast.LENGTH_SHORT).show()
            })

        queue.add(stringRequest)
    }

    private fun recupererInfosConducteur(distanceTotalParcourue: TextView) {
        val queue = Volley.newRequestQueue(this)
        val url = "use/your/script/recupererInfosConducteur.php"

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                val parts = response.split(";")
                val km_totaux = parts[0].toDoubleOrNull() ?: 0.0
                val date_obtention_permis = parts[1]
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val datePermis = sdf.parse(date_obtention_permis)
                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                calendar.time = datePermis
                val yearObtention = calendar.get(Calendar.YEAR)
                val yearsPassed = maxOf(1, currentYear - yearObtention)
                val moyenneKmAn = km_totaux / yearsPassed

                distanceTotalParcourue.text = "$km_totaux km parcourus soit environ ${"%.0f".format(moyenneKmAn)} km/an"
            },
            { error ->
                Log.e("Volley", "Erreur de requÃªte : ${error.message}")
                Toast.makeText(this, "ProblÃ¨me de rÃ©cupÃ©ration des infos", Toast.LENGTH_SHORT).show()
            })

        queue.add(stringRequest)
    }

    private fun getCurrentCar() {
        val queue = Volley.newRequestQueue(this)
        val url = "use/your/script/getCurrentCar.php"

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                ValuesManager.currentIDCar = response.toInt()
            },
            { error ->
                Log.e("Volley", "Erreur de requÃªte : ${error.message}")
                Toast.makeText(this, "ProblÃ¨me de rÃ©cupÃ©ration des infos", Toast.LENGTH_SHORT).show()
            })

        queue.add(stringRequest)
    }

    private fun recupererFindGasStation(context: Context, distanceMetres: Int, carburants: String) {
        val coordonnees = getLastKnownLocation(context)
        val latitude = coordonnees[0]
        val longitude = coordonnees[1]
        val queue = Volley.newRequestQueue(context)
        val url = "https://www.comparateur-prix-carburants.fr/comparateur-station-service/search/maps/all" +
                "?latitude=$latitude&longitude=$longitude&distanceKm=$distanceMetres&shortage=N&energies=$carburants&services=&compagnies="

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val jsonArray = JSONArray(response)
                    afficherResultatRecherche(context, formatStationData(jsonArray.toString()))
                } catch (e: JSONException) {
                    Toast.makeText(context, "Erreur JSON", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(context, "Erreur de requÃªte : ${error.message}", Toast.LENGTH_SHORT).show()
            })

        queue.add(stringRequest)
    }

    private fun afficherResultatRecherche(context: Context, resultatJson: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("RÃ©sultats de la recherche")

        val stations = parseJsonToStations(resultatJson)

        val scrollView = ScrollView(context)
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(20, 20, 20, 20)

        val textView = TextView(context)
        updateResultText(textView, stations)

        val btnSortDistance = Button(context).apply {
            text = "Trier par Distance"
            setOnClickListener {
                stations.sortBy { it.distance }
                updateResultText(textView, stations)
                textView.invalidate()
            }
        }

        val btnSortPrice = Button(context).apply {
            text = "Trier par Prix"
            setOnClickListener {
                stations.sortBy { it.prix }
                updateResultText(textView, stations)
                textView.invalidate()
            }
        }

        layout.addView(btnSortDistance)
        layout.addView(btnSortPrice)
        layout.addView(textView)
        scrollView.addView(layout)

        builder.setView(scrollView)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun traiterDonnees(data: String): List<DataPoint> {
        val dataPoints = mutableListOf<DataPoint>()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        val consommations = mutableListOf<Double>()

        for (valeur in data.split("|")) {
            val elements = valeur.split(";")
            if (elements.size >= 3) {
                try {
                    val date = dateFormat.parse(elements[0]) ?: continue
                    listeDates.add(elements[0])
                    listeDistances.add(elements[1])
                    listeVolumes.add(elements[2])
                    val distance = elements[1].toDouble()
                    val volume = elements[2].toDouble()
                    val consommation = volume * 100 / distance
                    consommations.add(consommation)

                    val timestamp = date.time.toDouble() / (1000 * 60 * 60 * 24)

                    dataPoints.add(DataPoint(timestamp, consommation))
                } catch (e: Exception) {
                    Log.e("Parsing", "Erreur conversion : ${e.message}")
                }
            }
        }

        textMean.text = String.format("%.3f", consommations.average()) + " L/100km"
        recordConsoValue = consommations.minOrNull() ?: 0.0

        return dataPoints
    }

    fun formatStationData(json: String): String {
        val jsonArray = JSONArray(json)
        val result = StringBuilder()

        for (i in 0 until jsonArray.length()) {
            val station = jsonArray.getJSONObject(i)

            val stationName = station.getString("name").trim().encodeToUtf8()
            val compagnyName = station.getString("compagny").trim().encodeToUtf8()
            val latitude = station.getDouble("latitude")
            val longitude = station.getDouble("longitude")

            result.append("* $stationName - $compagnyName ($latitude - $longitude):\n")

            val energyPrices = station.getJSONArray("energyPrices")
            if (energyPrices != null) {
                for (j in 0 until energyPrices.length()) {
                    val energyObject = energyPrices.getJSONObject(j)

                    val energyName = energyObject.getString("energy").encodeToUtf8()
                    val price = energyObject.getDouble("value")

                    result.append("    - $energyName: $price euros\n")
                }
            }
            result.append("\n")

        }
        return result.toString().trim()
    }

    fun String.encodeToUtf8(): String {
        return String(this.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
    }

    fun getLastKnownLocation(context: Context): List<String> {
        val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        val gps = DoubleArray(2)
        var location: Location? = null

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("getLastKnownLocation", "Location permissions are not granted.")
            return listOf("", "")
        }

        for (provider in providers) {
            location = locationManager.getLastKnownLocation(provider)
            if (location != null) {
                break
            }
        }

        if (location != null) {
            gps[0] = location.latitude * 100000
            gps[1] = location.longitude * 100000
            Log.d("getLastKnownLocation", "Latitude: ${gps[0]}, Longitude: ${gps[1]}")
            return listOf(gps[0].toString(), gps[1].toString())
        } else {
            Log.e("getLastKnownLocation", "Could not get the location.")
            return listOf("", "")
        }
    }

    fun graphe1(dataPoints: List<DataPoint>) {
        val series = LineGraphSeries(dataPoints.toTypedArray())
        series.color = resources.getColor(R.color.chart_color, null)

        series.setOnDataPointTapListener { _, dataPoint ->
            val index = dataPoints.indexOf(dataPoint)
            if (index in listeDates.indices) {
                afficherPopup(dataPoint as DataPoint, listeDates[index], listeDistances[index], listeVolumes[index])
            }
        }

        lineGraphView.gridLabelRenderer.apply {
            isHorizontalLabelsVisible = false
            isVerticalLabelsVisible = true
            gridStyle = GridLabelRenderer.GridStyle.BOTH
            gridColor = resources.getColor(R.color.white, null)
            verticalLabelsColor = resources.getColor(R.color.white, null)
        }

        lineGraphView.viewport.apply {
            isXAxisBoundsManual = true
            isScalable = true
            isScrollable = true
            setMinX(dataPoints.first().x)
            setMaxX(dataPoints.last().x)
        }

        lineGraphView.addSeries(series)
    }

    private fun afficherPopup(dataPoint: DataPoint, date: String, distance: String, volume: String) {
        val consommation = dataPoint.y
        val message = "Date: $date\nDistance: $distance km\nVolume: $volume L\nConsommation: ${"%.3f".format(consommation)} L/100km"

        val inflater = layoutInflater
        val layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_container))

        val textView = layout.findViewById<TextView>(R.id.toast_text)
        textView.text = message

        with (Toast(this)) {
            duration = Toast.LENGTH_LONG
            view = layout
            show()
        }
    }

    fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    data class Station(
        val nom: String,
        val distance: Double,
        val prix: Double
    )

    fun parseJsonToStations(json: String): MutableList<Station> {
        val stations = mutableListOf<Station>()

        val regexStation = """\* (.+?) \((\d+\.\d+) - (\d+\.\d+)\):""".toRegex()
        val regexPrix = """- (.+?): (\d+\.\d+) euros""".toRegex()

        val lines = json.split("\n")

        var currentStation: Station? = null
        for (line in lines) {
            val stationMatch = regexStation.find(line)
            val prixMatch = regexPrix.find(line)

            if (stationMatch != null) {
                val nom = stationMatch.groupValues[1]
                val distance = stationMatch.groupValues[2].toDouble() / 1000
                currentStation = Station(nom, distance, 0.0)
            } else if (prixMatch != null && currentStation != null) {
                val prix = prixMatch.groupValues[2].toDouble()
                currentStation = currentStation.copy(prix = prix)
                stations.add(currentStation)
            }
        }

        return stations
    }

    private fun updateResultText(textView: TextView, stations: List<Station>) {
        val resultText = stations.joinToString("\n") { "${it.nom} - ${it.distance} km - ${it.prix}â‚¬" }
        textView.text = resultText
    }
}
