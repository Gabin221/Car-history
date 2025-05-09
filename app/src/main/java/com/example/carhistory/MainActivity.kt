package com.example.carhistory

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.CheckedTextView
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
import com.jjoe64.graphview.series.PointsGraphSeries
import org.json.JSONArray
import org.json.JSONException
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private lateinit var buttonAccount: TextView
    private lateinit var titrePage: TextView
    private lateinit var buttonLeftCar: TextView
    private lateinit var buttonRightCar: TextView
    private lateinit var buttonCarburantCar: TextView
    private lateinit var buttonAddPlein: TextView
    private lateinit var lineGraphView: GraphView
    private lateinit var imageCar: ImageView
    private lateinit var textBuyDate: TextView
    private lateinit var textDistRun: TextView
    private lateinit var buttonSearchGasStation: TextView
    private lateinit var textMean: TextView
    private lateinit var distanceTotalParcourue: TextView
    private lateinit var recordConso: TextView
    private lateinit var distanceCarParcourue: TextView
    private lateinit var buttonReload: TextView

    private var listeDates = mutableListOf<String>()
    private var listeDistances = mutableListOf<String>()
    private var listeVolumes = mutableListOf<String>()
    private var listeSP = mutableListOf<String>()
    private var recordConsoValue = 0.0
    private var currentCarName = ""
    private var currentCarDist = 0.0
    private var latitudeUtilisateur = 0.0
    private var longitudeUtilisateur = 0.0
    private var facteurConversionAngles = 100000
    private var idCurrentCar = 1
    private var idMaxCar = 1
    private var idMinCar = 1
    private var nbrPointsGraphe = 15

    private val logoData = listOf(
        Pair(R.drawable.account_box_outline, "buttonAccount"),
        Pair(R.drawable.map_search_outline, "buttonSearchGasStation"),
        Pair(R.drawable.plus_circle_outline, "buttonAddPlein"),
        Pair(R.drawable.reload, "buttonReload")
    )

    @SuppressLint("SetTextI18n")
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
        buttonCarburantCar = findViewById(R.id.buttonCarburantCar)
        buttonAddPlein = findViewById(R.id.buttonAddPlein)
        lineGraphView = findViewById(R.id.lineGraphView)
        imageCar = findViewById(R.id.imageCar)
        textBuyDate = findViewById(R.id.textBuyDate)
        textDistRun = findViewById(R.id.textDistRun)
        buttonSearchGasStation = findViewById(R.id.buttonSearchGasStation)
        textMean = findViewById(R.id.textMean)
        buttonReload = findViewById(R.id.buttonReload)

        getCurrentCar { carId ->
            idMaxCar = carId
            idCurrentCar = carId
            
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
                    .setView(dialogView)

                val dialog: AlertDialog = builder.create()

                dialog.setOnShowListener {
                    dialog.window?.setBackgroundDrawableResource(R.color.background_card_add_plein)
                }

                dialog.show()
            }

            buttonLeftCar.setOnClickListener {
                if (idCurrentCar > idMinCar) {
                    idCurrentCar--
                    startFunctions(idCurrentCar)
                } else {
                    Toast.makeText(this, "Aucune voiture avant la $currentCarName", Toast.LENGTH_SHORT).show()
                }
            }

            buttonRightCar.setOnClickListener {
                if (idCurrentCar < idMaxCar) {
                    idCurrentCar++
                    startFunctions(idCurrentCar)
                } else {
                    Toast.makeText(this, "Aucune voiture après la $currentCarName", Toast.LENGTH_SHORT).show()
                }
            }

            buttonSearchGasStation.setOnClickListener {
                val builder = AlertDialog.Builder(this)
                val inflater = LayoutInflater.from(this)
                val dialogView = inflater.inflate(R.layout.formulaire_find_station, null)

                val editTextDistance = dialogView.findViewById<EditText>(R.id.editTextDistance)
                val listViewCarburants = dialogView.findViewById<ListView>(R.id.listViewCarburants)

                listViewCarburants.setOnItemClickListener { parent, view, position, id ->
                    val checkedTextView = view.findViewById<CheckedTextView>(R.id.checkedTextView)
                    checkedTextView.toggle()
                }

                val carburants = listOf("B7", "SP95-E5", "SP95-E10", "SP98-E5", "E85", "LPG")

                val adapter = ArrayAdapter(this, R.layout.item_carburant, R.id.checkedTextView, carburants)
                listViewCarburants.adapter = adapter
                listViewCarburants.choiceMode = ListView.CHOICE_MODE_MULTIPLE

                builder.setView(dialogView)
                    .setPositiveButton("Rechercher", null)
                    .setNegativeButton("Annuler") { dialog, _ -> dialog.dismiss() }

                val dialog: AlertDialog = builder.create()

                dialog.setOnShowListener {
                    dialog.window?.setBackgroundDrawableResource(R.color.background_card_add_plein)

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE)
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED)
                }

                dialog.show()

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
                    val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                    if (!isLocationEnabled) {
                        Toast.makeText(this, "La localisation est désactivée. Veuillez l'activer.", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }

                    val distanceKm = editTextDistance.text.toString().toDoubleOrNull() ?: 0.0
                    val distanceMetres = (distanceKm * 1000).toInt()

                    val selectedCarburants = mutableListOf<String>()
                    for (i in 0 until listViewCarburants.count) {
                        if (listViewCarburants.isItemChecked(i)) {
                            selectedCarburants.add(carburants[i])
                        }
                    }

                    if (selectedCarburants.isEmpty()) {
                        Toast.makeText(this, "Veuillez sélectionner au moins un carburant", Toast.LENGTH_SHORT).show()
                    } else {
                        val carburantsQuery = selectedCarburants.joinToString(",")
                        recupererFindGasStation(this, distanceMetres, carburantsQuery)
                        dialog.dismiss()
                    }
                }
            }

            buttonReload.setOnClickListener {
                Toast.makeText(this, "Chargement des données en cours...", Toast.LENGTH_SHORT).show()
                startFunctions(idCurrentCar)
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
                        val checkBoxSP98 = dialogView.findViewById<CheckBox>(R.id.checkBoxSP98)

                        val volume = editTextVolume.text.toString().toDoubleOrNull() ?: 0.0
                        val distance = editTextDistance.text.toString().toDoubleOrNull() ?: 0.0

                        val sp98 = if (checkBoxSP98.isChecked) 1 else 0

                        ajouterPlein(idCurrentCar, volume, distance, sp98)
                        modifierDistances(idCurrentCar, distance)
                    }
                    .setNegativeButton("Abandonner") { dialog, which ->
                        Toast.makeText(this, "Le plein ne sera pas ajouté.", Toast.LENGTH_SHORT).show()
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
            startFunctions(carId)
        }
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

    @SuppressLint("SetTextI18n")
    private fun recupererInfosVoitures(valeur_id: Int) {
        val queue = Volley.newRequestQueue(this)
        val url = "use/your/api?valeur_id=$valeur_id"

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
                buttonCarburantCar.text = parts[5]
                textBuyDate.text = dateAchat
                textDistRun.text = "${"%.0f".format(currentCarDist)} km"

                if (imageUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(imageUrl)
                        .into(imageCar)
                }

            },
            { error ->
                Log.e("Volley", "Erreur de requête : ${error.message}")
                Toast.makeText(this, "Problème de récupération des infos de la voiture", Toast.LENGTH_SHORT).show()
            })

        queue.add(stringRequest)
    }

    private fun ajouterPlein(valeur_id: Int, volume: Double, distance: Double, sp98: Int) {
        val date = getCurrentDate()

        val queue = Volley.newRequestQueue(this)
        val url = "use/your/api/ajouterPlein.php?valeur_id=$valeur_id&volume=$volume&distance=$distance&date=$date&sp98=$sp98"

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                Toast.makeText(this, "Le plein a été ajouté avec succès.", Toast.LENGTH_SHORT).show()
            },
            { error ->
                Log.e("Volley", "Erreur de requête : ${error.message}")
                Toast.makeText(this, "Problème lors de l'ajout du plein.", Toast.LENGTH_SHORT).show()
            })

        queue.add(stringRequest)
    }

    private fun recupererDonneesGraphe(valeur_id: Int) {
        val url = "use/your/api/recupererPleins.php?valeur_id=$valeur_id"

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
                Log.e("Volley", "Erreur requête : ${error.networkResponse?.statusCode} - ${error.message}")
            }
        )

        queue.add(stringRequest)
    }

    private fun modifierDistances(valeur_id: Int, distance: Double) {
        val queue = Volley.newRequestQueue(this)
        val url = "use/your/api/modifierDistance.php?valeur_id=$valeur_id&distance=$distance"

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                Toast.makeText(this, "Les distances ont été mises à jour avec succès.", Toast.LENGTH_SHORT).show()
            },
            { error ->
                Log.e("Volley", "Erreur de requête : ${error.message}")
                Toast.makeText(this, "Problème lors de la mise à jour des distances.", Toast.LENGTH_SHORT).show()
            })

        queue.add(stringRequest)
    }

    @SuppressLint("SetTextI18n")
    private fun recupererInfosConducteur(distanceTotalParcourue: TextView) {
        val queue = Volley.newRequestQueue(this)
        val url = "use/your/api/recupererInfosConducteur.php"

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
                Log.e("Volley", "Erreur de requête : ${error.message}")
                Toast.makeText(this, "Problème de récupération des infos du conducteur", Toast.LENGTH_SHORT).show()
            })

        queue.add(stringRequest)
    }

    private fun getCurrentCar(callback: (Int) -> Unit) {
        val queue = Volley.newRequestQueue(this)
        val url = "use/your/api/getCurrentCar.php"

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                val carId = response.toInt()
                callback(carId)
            },
            { error ->
                Log.e("Volley", "Erreur de requête : ${error.message}")
                Toast.makeText(this, "Problème de récupération la voiture actuelle", Toast.LENGTH_SHORT).show()
            }
        )

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
                    Toast.makeText(context, "Erreur JSON: $e", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(context, "Erreur de requête : ${error.message}", Toast.LENGTH_SHORT).show()
            })

        queue.add(stringRequest)
    }

    @SuppressLint("SetTextI18n")
    private fun afficherResultatRecherche(context: Context, resultatJson: String) {
        val builder = AlertDialog.Builder(context)

        val title = SpannableString("Résultats de la recherche")
        title.setSpan(ForegroundColorSpan(Color.WHITE), 0, title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setTitle(title)

        val stations = parseJsonToStations(resultatJson)

        val scrollView = ScrollView(context)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        val textView = TextView(context)
        updateResultText(textView, stations)

        val sortLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 10)
        }

        val txtSortDistance = TextView(context).apply {
            text = "Tri par \uD83D\uDCCF"
            setTextColor(Color.LTGRAY)
            textSize = 16f
            setPadding(0, 0, 0, 0)
            setOnClickListener {
                stations.sortBy { it.distance }
                updateResultText(textView, stations)
                textView.invalidate()
            }
        }

        val txtSortPrice = TextView(context).apply {
            text = "Tri par 💰"
            setTextColor(Color.LTGRAY)
            textSize = 16f
            setPadding(60, 0, 0, 0)
            setOnClickListener {
                stations.sortBy { it.prix }
                updateResultText(textView, stations)
                textView.invalidate()
            }
        }

        sortLayout.addView(txtSortDistance)
        sortLayout.addView(txtSortPrice)

        layout.addView(sortLayout)
        layout.addView(textView)
        textView.setTextColor(Color.WHITE)
        scrollView.addView(layout)

        builder.setView(scrollView)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        val dialog: AlertDialog = builder.create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(R.color.background_card_add_plein)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE)
        }

        dialog.show()
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
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
                    listeSP.add(elements[3])
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
            val latitude = station.getDouble("latitude")
            val longitude = station.getDouble("longitude")

            result.append("* $stationName ($latitude - $longitude):\n")

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
            gps[0] = location.latitude * facteurConversionAngles
            gps[1] = location.longitude * facteurConversionAngles
            Log.d("getLastKnownLocation", "Latitude: ${gps[0]}, Longitude: ${gps[1]}")
            latitudeUtilisateur = gps[0]
            longitudeUtilisateur = gps[1]
            return listOf(gps[0].toString(), gps[1].toString())
        } else {
            Log.e("getLastKnownLocation", "Could not get the location.")
            return listOf("", "")
        }
    }

    fun graphe1(dataPoints: List<DataPoint>) {
        val displayedPoints = if (dataPoints.size > nbrPointsGraphe) dataPoints.takeLast(nbrPointsGraphe) else dataPoints

        val series = LineGraphSeries(displayedPoints.toTypedArray())
        series.color = resources.getColor(R.color.chart_color, null)

        val pointsSeries = PointsGraphSeries(displayedPoints.toTypedArray())
        pointsSeries.size = 8f

        pointsSeries.setCustomShape { canvas, paint, x, y, dataPoint ->
            val index = dataPoints.indexOf(dataPoint)
            if (index in listeSP.indices) {
                paint.color = if (listeSP[index] == "1") {
                    resources.getColor(R.color.chart_color_sp98, null)
                } else {
                    resources.getColor(R.color.chart_color, null)
                }
                canvas.drawCircle(x, y, pointsSeries.size, paint)
            }
        }

        series.setOnDataPointTapListener { _, dataPoint ->
            val index = dataPoints.indexOf(dataPoint)
            if (index in listeDates.indices) {
                afficherPopup(dataPoint as DataPoint, listeDates[index], listeDistances[index], listeVolumes[index], listeSP[index])
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
            val yValues = displayedPoints.map { it.y }
            val minY = yValues.minOrNull() ?: 0.0
            val maxY = yValues.maxOrNull() ?: 1.0
            isXAxisBoundsManual = true
            isScalable = true
            isScrollable = true
            setMinX(displayedPoints.first().x)
            setMaxX(displayedPoints.last().x)
            isYAxisBoundsManual = true
            setMinY(0.85 * minY)
            setMaxY(1.15 * maxY)
        }

        lineGraphView.addSeries(series)
        lineGraphView.addSeries(pointsSeries)
    }

    private fun afficherPopup(dataPoint: DataPoint, date: String, distance: String, volume: String, sp: String) {
        val consommation = dataPoint.y
        val typeCarburant = if (sp == "1") "SP98" else "SP95"
        val message = "Date: $date\nDistance: $distance km\nVolume: $volume L\nConsommation: ${"%.3f".format(consommation)} L/100km\nCarburant: $typeCarburant"

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
        var currentCarburant: String? = null

        for (line in lines) {
            val stationMatch = regexStation.find(line)
            val prixMatch = regexPrix.find(line)

            if (stationMatch != null) {
                val nom = stationMatch.groupValues[1]
                val latStation = stationMatch.groupValues[2].toDouble() / facteurConversionAngles
                val lonStation = stationMatch.groupValues[3].toDouble() / facteurConversionAngles

                val distance = round(calculerDistance(latitudeUtilisateur / facteurConversionAngles, longitudeUtilisateur / facteurConversionAngles, latStation, lonStation) * 100) / 100

                currentStation = Station(nom, distance, 0.0)
            } else if (prixMatch != null && currentStation != null) {
                currentCarburant = prixMatch.groupValues[1]
                val prix = prixMatch.groupValues[2].toDouble()

                val stationAvecCarburant = currentStation.copy(
                    nom = "[$currentCarburant] ${currentStation.nom}",
                    prix = prix
                )
                if (!stationAvecCarburant.nom.contains("null")) {
                    stations.add(stationAvecCarburant)
                }
            }
        }

        return stations
    }

    private fun updateResultText(textView: TextView, stations: List<Station>) {
        val resultText = stations.joinToString("\n") { "* ${it.nom} - ${it.distance} km - ${it.prix}€\n" }
        textView.text = resultText
    }

    fun calculerDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371e3
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaphi = Math.toRadians(lat2 - lat1)
        val deltalambda = Math.toRadians(lon2 - lon1)

        val a = sin(deltaphi / 2) * sin(deltaphi / 2) +
                cos(phi1) * cos(phi2) * sin(deltalambda / 2) * sin(deltalambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c / 1000
    }
}