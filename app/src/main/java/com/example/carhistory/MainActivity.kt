﻿package com.example.carhistory

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
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

    private val logoData = listOf(
        Pair(R.drawable.account_box_outline, "buttonAccount"),
        Pair(R.drawable.car_wrench, "iconMaintenance"),
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

        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.find_gas_stations, null)
        textCoordinates = dialogView.findViewById(R.id.textCoordinates)

        recupererFindGasStation()

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setView(dialogView)

        val dialog: AlertDialog = builder.create()
        dialog.show()

        buttonAccount.setOnClickListener {
            val inflater = this.layoutInflater
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder
                .setView(inflater.inflate(R.layout.stats_window_modale, null))

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }

        buttonLeftCar.setOnClickListener {
            recupererInfosVoitures(ValuesManager.currentIDCar - 1)
            ValuesManager.currentIDCar--
        }

        buttonRightCar.setOnClickListener {
            recupererInfosVoitures(ValuesManager.currentIDCar + 1)
            ValuesManager.currentIDCar++
        }

        buttonSearchGasStation.setOnClickListener {
            val inflater = this.layoutInflater
            val dialogView = inflater.inflate(R.layout.find_gas_stations, null)
            textCoordinates = dialogView.findViewById(R.id.textCoordinates)

            recupererFindGasStation()

            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setView(dialogView)

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }

        buttonAddPlein.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder
                .setTitle("Ajouter un plein")
                .setMessage("I am the message")
                .setPositiveButton("Positive") { dialog, which ->
                    // Do something.
                }
                .setNegativeButton("Negative") { dialog, which ->
                    // Do something else.
                }

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }

        initLogo()
        recupererInfosVoitures(ValuesManager.currentIDCar)
        recupererDonneesGraphe(ValuesManager.currentIDCar)
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
                val distance = parts[4]
                titrePage.text = titre
                buttonCenterCar.text = titre
                textBuyDate.text = dateAchat
                textDistRun.text = distance + " km"

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

    private fun recupererFindGasStation() {
        val coordonnees = getLastKnownLocation(this)
        val latitude = coordonnees[0]
        val longitude = coordonnees[1]
        val queue = Volley.newRequestQueue(this)
        val url = "https://www.comparateur-prix-carburants.fr/comparateur-station-service/search/maps/all?latitude=$latitude&longitude=$longitude&distanceKm=20000&shortage=N&energies=SP95-E5,SP95-E10&services=&compagnies="

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val jsonArray = JSONArray(response)
                    textCoordinates.text = formatStationData(jsonArray.toString())
                } catch (e: JSONException) {
                    Log.e("Volley", "Erreur JSON : ${e.message}")
                    textCoordinates.text = "Erreur JSON"
                }
            },
            { error ->
                Log.e("Volley", "Erreur de requÃªte : ${error.networkResponse?.statusCode} - ${error.message}")
                if (error.networkResponse != null) {
                    Log.e("Volley", "RÃ©ponse serveur : ${String(error.networkResponse.data)}")
                }
                Toast.makeText(this, "ProblÃ¨me de rÃ©cupÃ©ration des stations essences.", Toast.LENGTH_SHORT).show()
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

    private fun traiterDonnees(data: String): List<DataPoint> {
        val dataPoints = mutableListOf<DataPoint>()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) // Format de tes dates

        for (valeur in data.split("|")) {
            val elements = valeur.split(";")
            if (elements.size >= 3) {
                try {
                    val date = dateFormat.parse(elements[0]) ?: continue // Convertir la date
                    val distance = elements[1].toDouble()
                    val volume = elements[2].toDouble()
                    val consommation = volume * 100 / distance // Calcul dÃ©jÃ  fait

                    // Convertir la date en timestamp (nombre de jours depuis 1970)
                    val timestamp = date.time.toDouble() / (1000 * 60 * 60 * 24) // Convertir en jours

                    dataPoints.add(DataPoint(timestamp, consommation))
                } catch (e: Exception) {
                    Log.e("Parsing", "Erreur conversion : ${e.message}")
                }
            }
        }
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

        lineGraphView.gridLabelRenderer.apply {
            isHorizontalLabelsVisible = true
            isVerticalLabelsVisible = true
            verticalLabelsColor = resources.getColor(R.color.white, null)
            horizontalLabelsColor = resources.getColor(R.color.white, null)
            labelFormatter = object : DefaultLabelFormatter() {
                private val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

                override fun formatLabel(value: Double, isValueX: Boolean): String {
                    return if (isValueX) dateFormat.format(Date(value.toLong())) else super.formatLabel(value, isValueX)
                }
            }
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
}
