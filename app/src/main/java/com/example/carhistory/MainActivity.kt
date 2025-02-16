package com.example.carhistory

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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import org.json.JSONArray
import org.json.JSONException

class MainActivity : AppCompatActivity() {

    private lateinit var buttonAccount: TextView
    private lateinit var titrePage: TextView
    private lateinit var buttonLeftCar: TextView
    private lateinit var buttonRightCar: TextView
    private lateinit var buttonCenterCar: TextView
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
        Pair(R.drawable.plus_circle_outline, "buttonNew")
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
        lineGraphView = findViewById(R.id.lineGraphView)
        imageCar = findViewById(R.id.imageCar)
        textBuyDate = findViewById(R.id.textBuyDate)
        textDistRun = findViewById(R.id.textDistRun)
        buttonSearchGasStation = findViewById(R.id.buttonSearchGasStation)

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

        /*
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
        */

        initLogo()
        graphe1()
        recupererInfosVoitures(2)
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
        val url = "use/your/api?valeur_id=$valeur_id"

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
                Log.e("Volley", "Erreur de requête : ${error.message}")
                Toast.makeText(this, "Problème de récupération des infos", Toast.LENGTH_SHORT).show()
            })

        queue.add(stringRequest)
    }

    private fun recupererFindGasStation() {
        val queue = Volley.newRequestQueue(this)
        val url = "https://www.comparateur-prix-carburants.fr/comparateur-station-service/search/maps/all?latitude=4726141.793175175&longitude=596917.4087706337&distanceKm=20000&shortage=N&energies=SP95-E5,SP95-E10&services=&compagnies="

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val jsonArray = JSONArray(response)
                    textCoordinates.text = jsonArray.getString(1).toString()
                } catch (e: JSONException) {
                    Log.e("Volley", "Erreur JSON : ${e.message}")
                    textCoordinates.text = "Erreur JSON"
                }
            },
            { error ->
                Log.e("Volley", "Erreur de requête : ${error.networkResponse?.statusCode} - ${error.message}")
                if (error.networkResponse != null) {
                    Log.e("Volley", "Réponse serveur : ${String(error.networkResponse.data)}")
                }
                Toast.makeText(this, "Problème de récupération des stations essences.", Toast.LENGTH_SHORT).show()
            })

        queue.add(stringRequest)
    }

    fun graphe1() {
        val series: LineGraphSeries<DataPoint> = LineGraphSeries(
            arrayOf(
                DataPoint(0.0, 10.0),
                DataPoint(1.0, 12.0),
                DataPoint(2.0, 7.0),
                DataPoint(3.0, 6.0),
                DataPoint(4.0, 11.0),
                DataPoint(5.0, 15.0),
                DataPoint(6.0, 16.0),
                DataPoint(7.0, 14.0),
                DataPoint(8.0, 17.0),
                DataPoint(9.0, 15.0),
                DataPoint(10.0, 13.0),
                DataPoint(11.0, 14.0)
            )
        )

        series.color = resources.getColor(R.color.chart_color, null)

        lineGraphView.gridLabelRenderer.apply {
            isHorizontalLabelsVisible = true
            isVerticalLabelsVisible = true
            verticalLabelsColor = resources.getColor(R.color.white, null)
            horizontalLabelsColor = resources.getColor(R.color.white, null)
            gridColor = resources.getColor(R.color.white, null)
        }

        lineGraphView.viewport.isScrollable = false
        lineGraphView.viewport.isScalable = false
        lineGraphView.viewport.setScalableY(false)
        lineGraphView.viewport.setScrollableY(false)

        lineGraphView.addSeries(series)
    }
}