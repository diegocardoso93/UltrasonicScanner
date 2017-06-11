package br.unisc.sisemb.ultrasonicscanner

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem

class MainActivity : AppCompatActivity() {
    companion object {
        private val RESULT_BLE_OK = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val fab = findViewById(R.id.bluetooth) as FloatingActionButton
        fab.setOnClickListener { view ->
            val deviceScanIntent = Intent(this, DeviceScanActivity::class.java)
            startActivityForResult(deviceScanIntent, RESULT_BLE_OK)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_BLE_OK) {
            Log.d("DEVICE_NAME", data.getStringExtra("DEVICE_NAME"))
            Log.d("DEVICE_ADDRESS", data.getStringExtra("DEVICE_ADDRESS"))
        }
    }


}
