package com.example.carhistory

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ImageSpan
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.GridLabelRenderer
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries


class MainActivity : AppCompatActivity() {

    private lateinit var buttonAccount: TextView

    private val logoData = listOf(
        Pair(R.drawable.account_box_outline, "buttonAccount")
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

        buttonAccount.setOnClickListener {
            val inflater = this.layoutInflater
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder
                .setView(inflater.inflate(R.layout.stats_window_modale, null))

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }

        /*
        buttonCarInfo.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder
                .setTitle("Fiche technique")
                .setMessage("I am the message")

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
         */

        initLogo()
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

    /*
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
            isHorizontalLabelsVisible = false
            isVerticalLabelsVisible = true
            gridStyle = GridLabelRenderer.GridStyle.NONE
            verticalLabelsColor = resources.getColor(R.color.white, null)
        }

        lineGraphView.viewport.isScrollable = false
        lineGraphView.viewport.isScalable = false
        lineGraphView.viewport.setScalableY(false)
        lineGraphView.viewport.setScrollableY(false)

        lineGraphView.addSeries(series)
    }*/
}