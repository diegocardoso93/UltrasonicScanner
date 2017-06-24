package br.unisc.sisemb.ultrasonicscanner

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText

class SettingsActivity : AppCompatActivity() {

    val RESULT_SETTINGS_SELECTED: Int = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }

    fun setRefreshRate(v: View) {
        val etRefreshRate: EditText = findViewById(R.id.etRefreshRate) as EditText
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("REFRESH_RATE_SELECTED", if (etRefreshRate.text.toString().length>0) etRefreshRate.text.toString() else "5")
        setResult(RESULT_SETTINGS_SELECTED, intent)
        finish()
    }

    fun setMaxDistance(v: View) {
        val etMaxDistance: EditText = findViewById(R.id.etMaxDistance) as EditText
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("MAX_DISTANCE_SELECTED", if (etMaxDistance.text.toString().length>0) etMaxDistance.text.toString() else "200")
        setResult(RESULT_SETTINGS_SELECTED, intent)
        finish()
    }

    fun setStop(v: View) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("STOP_SELECTED", 1)
        setResult(RESULT_SETTINGS_SELECTED, intent)
        finish()
    }

}
